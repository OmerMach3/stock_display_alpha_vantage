package com.finansal.finansal_deneme.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.finansal.finansal_deneme.dto.external.AlphaVantageMonthlyResponseDto;
import com.finansal.finansal_deneme.dto.external.AlphaVantageDailyResponseDto;
import com.finansal.finansal_deneme.dto.external.TimeSeriesEntryDto;
import com.finansal.finansal_deneme.integration.FinancialDataProvider;
import com.finansal.finansal_deneme.model.StockData;
import com.finansal.finansal_deneme.model.StockDailyData;
import com.finansal.finansal_deneme.repository.StockDataRepository;
import com.finansal.finansal_deneme.repository.StockDailyDataRepository;
import com.finansal.finansal_deneme.service.StockService;

//Serice sınıfı hisse senedi işlemlerinin ana merkezi
@Service
@Validated // Enables method parameter validation such as @NotBlank on service methods (IoC-friendly input validation)
public class StockServiceImpl implements StockService {
    private final StockDataRepository stockDataRepository;
    private final FinancialDataProvider financialDataProvider;
    private final StockDailyDataRepository stockDailyDataRepository;

    private static final Logger log = LoggerFactory.getLogger(StockServiceImpl.class);

    // Constructor - Spring otomatik olarak gerekli bağımlılıkları enjekte ediyor
    public StockServiceImpl(StockDataRepository stockDataRepository,
                            FinancialDataProvider financialDataProvider,
                            StockDailyDataRepository stockDailyDataRepository) {
        this.stockDataRepository = stockDataRepository;
        this.financialDataProvider = financialDataProvider;
        this.stockDailyDataRepository = stockDailyDataRepository;
    }

    // Eski intraday özelliği kaldırıldı, sadece günlük ve aylık veri kaldı

    // AYLIK VERİ SENKRONIZASYONU - Alpha Vantage'den aylık verileri çekip veritabanına kaydediyor
    @Override
    @Async("taskExecutor") // Arka planda çalışıyor, main thread'i bloklamıyor 
    @Transactional // Bir hata olursa tüm işlem geri alınıyor (atomik işlem)
    @CacheEvict(cacheNames = {"stocksBySymbol", "symbols"}, allEntries = true) // Cache'i temizliyor ki eski data göstermesin
    public void syncMonthlyStockData(String symbol) {
        // @NotBlank + @Validated sayesinde boş string kontrolü otomatik yapılıyor
        String normalizedSymbol = symbol.trim().toUpperCase(); // Hep büyük harfle tutalım, tutarlılık için
        log.info("Aylık senkronizasyon başlatılıyor: {}", normalizedSymbol);
        
        try {
            // API'den monthly veriyi çek - Alpha Vantage
            Optional<AlphaVantageMonthlyResponseDto> responseOpt = 
                financialDataProvider.fetchMonthlyStockData(normalizedSymbol);
            
            // API'den veri gelmedi mi? 
            if (responseOpt.isEmpty()) {
                log.warn("Monthly data alınamadı: {}", normalizedSymbol);
                return;
            }
            
            AlphaVantageMonthlyResponseDto response = responseOpt.get();

            // Aylık zaman serisi verisini işle ve kaydet - asıl iş burada
            if (response.getMonthlyTimeSeries() != null && !response.getMonthlyTimeSeries().isEmpty()) {
                // Veriyi satır satır kaydetmek için bu metodu kullanıyoruz
                saveMonthlyTimeSeriesData(normalizedSymbol, response.getMonthlyTimeSeries());

                log.info("Aylık veri işlendi ve kaydedildi: {} ({} aylık veri)", normalizedSymbol,
                        response.getMonthlyTimeSeries().size());
            }

            log.info("Aylık senkronizasyon tamamlandı: {}", normalizedSymbol);

        } catch (Exception e) {
            // Hata loglamak çok önemli - production'da ne olduğunu anlamak için
            log.error("Aylık senkronizasyon sırasında bir hata oluştu ({}): {}", normalizedSymbol, e.getMessage(), e);
        }
    }

    // GÜNLÜK VERİ SENKRONIZASYONU - Her gün için ayrı kayıt oluşturuyor (son 1000 gün)
    @Override
    @Async("taskExecutor") // Bu da arka planda çalışıyor
    @Transactional // Atomik işlem garantisi
    @CacheEvict(cacheNames = {"stocksBySymbol", "symbols"}, allEntries = true) // Cache temizliği
    public void syncDailyStockData(String symbol) {
        String normalizedSymbol = symbol.trim().toUpperCase(); // Tutarlılık için normalize et
        log.info("Günlük senkronizasyon başlatılıyor: {}", normalizedSymbol);
        
        try {
            // Alpha Vantage'den günlük verileri çek
            Optional<AlphaVantageDailyResponseDto> responseOpt =
                financialDataProvider.fetchDailyStockData(normalizedSymbol);

            if (responseOpt.isEmpty()) {
                log.warn("Daily data alınamadı: {}", normalizedSymbol);
                return;
            }

            AlphaVantageDailyResponseDto response = responseOpt.get();
            Map<String, TimeSeriesEntryDto> series = response.getDailyTimeSeries();
            
            if (series != null && !series.isEmpty()) {
                // AKILLI FİLTRELEME - Sadece son 1000 günü alıyoruz (veritabanı şişmesin)
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                
                // Stream API ile en yeni 1000 günü al - performans odaklı
                Map<String, TimeSeriesEntryDto> limitedSeries = series.entrySet().stream()
                    .sorted((a, b) -> LocalDate.parse(b.getKey(), formatter)  // Yeniden eskiye sıralama
                                  .compareTo(LocalDate.parse(a.getKey(), formatter)))
                    .limit(1000) // İlk 1000 tanesini al (en yeniler)
                    .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1, // Çakışma durumunda ilkini al
                        LinkedHashMap::new // Sıralama korunsun
                    ));

                // Filtrelenmiş veriyi veritabanına kaydet
                saveDailyTimeSeriesData(normalizedSymbol, limitedSeries);
                log.info("Günlük veri işlendi ve kaydedildi: {} ({} günlük veri)", 
                        normalizedSymbol, limitedSeries.size());
            }

            log.info("Günlük senkronizasyon tamamlandı: {}", normalizedSymbol);
            
        } catch (Exception e) {
            // Hata varsa logla - debugging için çok önemli
            log.error("Günlük senkronizasyon sırasında bir hata oluştu ({}): {}", 
                     normalizedSymbol, e.getMessage(), e);
        }
    }

    // AYLIK VERİ KAYDETME - Alpha Vantage'den gelen aylık verileri veritabanına kaydediyor
    @Override
    @Transactional // Tek seferde tümü ya hiçbiri - atomik işlem
    public void saveMonthlyTimeSeriesData(String symbol, Map<String, TimeSeriesEntryDto> timeSeries) {
        if (timeSeries == null || timeSeries.isEmpty()) {
            return; // Boşsa yapacak bir şey yok
        }

        String normalizedSymbol = symbol.trim().toUpperCase(); // Standart format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // Tarih format

        // PERFORMANS TRİKİ: Önce hangi tarihlerin geldiğini belirle
        Set<LocalDate> targetDates = timeSeries.keySet().stream()
            .map(dateStr -> LocalDate.parse(dateStr, formatter)) // String'den LocalDate'e çevir
            .collect(Collectors.toSet());

        // Veritabanından mevcut kayıtları al - gereksiz insert'ları önlemek için
        Map<LocalDate, StockData> existingByDate = stockDataRepository
            .findBySymbolAndDateIn(normalizedSymbol, targetDates)
            .stream()
            .collect(Collectors.toMap(StockData::getDate, Function.identity())); // Map'e çevir

        List<StockData> stockDataList = new ArrayList<>(timeSeries.size());

        // Her tarih için veri işle - lambda ile hızlı
        timeSeries.forEach((dateStr, entryDto) -> {
            LocalDate parsedDate = LocalDate.parse(dateStr, formatter); // Direct parse - cache gerek yok
            
            // Mevcut kayıt varsa onu al, yoksa yeni oluştur
            StockData stockData = existingByDate.getOrDefault(parsedDate, new StockData());
            if (stockData.getId() == null) { // Yeni kayıt ise temel bilgileri set et
                stockData.setSymbol(normalizedSymbol);
                stockData.setDate(parsedDate);
            }
            
            // Fiyat bilgilerini güncelle - hem yeni hem mevcut kayıtlar için
            stockData.setOpen(entryDto.getOpen());
            stockData.setHigh(entryDto.getHigh());
            stockData.setLow(entryDto.getLow());
            stockData.setClose(entryDto.getClose());
            stockData.setVolume(entryDto.getVolume());

            stockDataList.add(stockData); // Listeye ekle
        });

        // TOPLU KAYDETME - Performans için batch'lerde kaydet
        saveInBatches(stockDataRepository, stockDataList, 1000);
    }

    // YARDIMCI METOD - Büyük veri setlerini küçük parçalarda kaydediyor (memory overflow önlemek için)
    private <T> void saveInBatches(JpaRepository<T, ?> repository, List<T> entityList, int batchSize) {
        if (entityList == null || entityList.isEmpty()) {
            return; // Boşsa çık
        }
        
        // Listeyi küçük parçalara böl ve teker teker kaydet
        for (int i = 0; i < entityList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, entityList.size()); // Son batch küçük olabilir
            List<T> batch = entityList.subList(i, end); // Parça al
            repository.saveAll(batch); // Toplu kaydet
            repository.flush(); // Hemen veritabanına gönder - memory temizle
        }
    }

    // GÜNLÜK VERİ KAYDETME - Günlük verileri ayrı tabloya kaydediyor (1000 günlük limit ile)
    @Override
    @Transactional // Güvenli kaydetme için atomik işlem
    public void saveDailyTimeSeriesData(String symbol, Map<String, TimeSeriesEntryDto> timeSeries) {
        if (timeSeries == null || timeSeries.isEmpty()) {
            return; // Boşsa dur
        }

        String normalizedSymbol = symbol.trim().toUpperCase(); // Standart format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // Tarih çeviricisi

        // PERFORMANS BOOST 1: Tarihleri tek seferde çevir ve tekrarları kaldır
        Set<LocalDate> targetDates = timeSeries.keySet().stream()
                .map(dateStr -> LocalDate.parse(dateStr, formatter)) // String'i LocalDate'e çevir
                .collect(Collectors.toSet()); // Set'e topla (unique değerler)

        // PERFORMANS BOOST 2: Mevcut kayıtları tek sorguda al ve Map'e çevir
        Map<LocalDate, StockDailyData> existingByDate = stockDailyDataRepository
                .findBySymbolAndDateIn(normalizedSymbol, targetDates)
                .stream()
                .collect(Collectors.toMap(StockDailyData::getDate, Function.identity())); // Tarih-Entity map'i

        // PERFORMANS BOOST 3: Liste boyutunu önceden ayarla - memory optimizasyonu
        List<StockDailyData> persistList = new ArrayList<>(timeSeries.size());

        // Her günlük veri için işlem yap - lambda ile hızlı
        timeSeries.forEach((dateStr, entryDto) -> {
            LocalDate parsedDate = LocalDate.parse(dateStr, formatter); // Parse et
            
            // Mevcut kayıt varsa al, yoksa yeni oluştur
            StockDailyData item = existingByDate.getOrDefault(parsedDate, new StockDailyData());
            if (item.getId() == null) { // Yeni kayıt ise temel bilgileri set et
                item.setSymbol(normalizedSymbol);
                item.setDate(parsedDate);
            }
            
            // Fiyat verilerini güncelle - hem yeni hem mevcut kayıtlar için
            item.setOpen(entryDto.getOpen());
            item.setHigh(entryDto.getHigh());
            item.setLow(entryDto.getLow());
            item.setClose(entryDto.getClose());
            item.setVolume(entryDto.getVolume());
            
            persistList.add(item); // Listeye ekle
        });

        // TOPLU KAYDETME - Büyük veri setleri için batch kaydetme kullan
        saveInBatches(stockDailyDataRepository, persistList, 1000);
    }

    // VERİ OKUMA METODLARİ - Cached ve optimized

    // AYLIK VERİ GETİRME - Cache'li ve hızlı
    @Override
    @Transactional(readOnly = true) // Sadece okuma - performans artışı
    @Cacheable(cacheNames = "stocksBySymbol", key = "#symbol.trim().toUpperCase()") // Cache'de tut - tekrar okumaya gerek yok
    public List<StockData> getStockDataBySymbol(String symbol) {
        // Sembolü normalize et ve veritabanından getir - yeniden eskiye sıralı
        return stockDataRepository.findBySymbolOrderByDateDesc(symbol.trim().toUpperCase());
    }

    @Override
    @Transactional(readOnly = true) // Read-only transactional context improves performance and expresses intent
    @Cacheable(cacheNames = "symbols") // Cache distinct symbols list
    public List<String> getAllSymbols() {
        // Veritabanındaki tüm farklı sembolleri getir.
        return stockDataRepository.findDistinctSymbols();
    }

    // GÜNLÜK VERİ GETİRME - Belirli bir sembol için günlük verileri getir
    @Override
    @Transactional(readOnly = true) // Sadece okuma - güvenli ve hızlı
    public List<StockDailyData> getDailyStockDataBySymbol(String symbol) {
        // Sembolü temizle, büyük harfe çevir ve günlük verileri getir - yeniden eskiye sıralı
        return stockDailyDataRepository.findBySymbolOrderByDateDesc(symbol.trim().toUpperCase());
    }


}