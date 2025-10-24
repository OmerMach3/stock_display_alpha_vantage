package com.finansal.finansal_deneme.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finansal.finansal_deneme.dto.external.AlphaVantageMonthlyResponseDto;
import com.finansal.finansal_deneme.dto.external.TimeSeriesEntryDto;
import com.finansal.finansal_deneme.integration.FinancialDataProvider;
import com.finansal.finansal_deneme.model.StockData;
import com.finansal.finansal_deneme.repository.StockDataRepository;
import com.finansal.finansal_deneme.service.StockService;

@Service
public class StockServiceImpl implements StockService {
    
    private final StockDataRepository stockDataRepository;
    private final FinancialDataProvider financialDataProvider;

    public StockServiceImpl(StockDataRepository stockDataRepository,
                           FinancialDataProvider financialDataProvider) {
        this.stockDataRepository = stockDataRepository;
        this.financialDataProvider = financialDataProvider;
    }

    @Override
    public void syncStockData(String symbol) {
        syncMonthlyStockData(symbol);
    }

    @Override
    @Async("taskExecutor")
    @Transactional
    public void syncMonthlyStockData(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            System.err.println("Monthly senkronizasyon hatası: sembol boş olamaz.");
            return;
        }

        String normalizedSymbol = symbol.trim().toUpperCase();
        System.out.println("Monthly senkronizasyon başlatılıyor: " + normalizedSymbol);
        
        try {
            // API'den monthly veriyi çek
            Optional<AlphaVantageMonthlyResponseDto> responseOpt = 
                financialDataProvider.fetchMonthlyStockData(normalizedSymbol);
            
            if (responseOpt.isEmpty()) {
                System.err.println("Monthly data alınamadı: " + normalizedSymbol);
                return;
            }
            
            AlphaVantageMonthlyResponseDto response = responseOpt.get();

            // Aylık zaman serisi verisini işle ve kaydet.
            if (response.getMonthlyTimeSeries() != null && !response.getMonthlyTimeSeries().isEmpty()) {
                // Veriyi satır satır kaydetmek için bu metodu kullanıyoruz.
                saveTimeSeriesData(normalizedSymbol, response.getMonthlyTimeSeries());

                System.out.println("Aylık veri işlendi ve kaydedildi: " + normalizedSymbol +
                    " (" + response.getMonthlyTimeSeries().size() + " aylık veri)");
            }

            System.out.println("Aylık senkronizasyon tamamlandı: " + normalizedSymbol);

        } catch (Exception e) {
            System.err.println("Aylık senkronizasyon sırasında bir hata oluştu (" + normalizedSymbol + "): " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    @Transactional
    public void saveTimeSeriesData(String symbol, Map<String, TimeSeriesEntryDto> timeSeries) {
        if (timeSeries == null || timeSeries.isEmpty()) {
            return;
        }

        String normalizedSymbol = symbol.trim().toUpperCase();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Gelen verideki tarihleri topla.(hashmap ile complexity'yi azaltıyoruz. log(n*m)->O(n)))
        Set<String> rawDates = new HashSet<>(timeSeries.keySet());
        Set<LocalDate> targetDates = new HashSet<>();
        for (String dateStr : rawDates) {
            targetDates.add(LocalDate.parse(dateStr, formatter));
        }

        // Bu tarihlerde veritabanında zaten kayıt var mı diye kontrol et.
        List<StockData> existingRecords = stockDataRepository.findBySymbolAndDateIn(normalizedSymbol, targetDates);
        Map<LocalDate, StockData> existingByDate = new HashMap<>();
        for (StockData existing : existingRecords) {
            existingByDate.put(existing.getDate(), existing);
        }

        List<StockData> stockDataList = new ArrayList<>(timeSeries.size());

        // Gelen her bir zaman serisi verisi için...
        for (Map.Entry<String, TimeSeriesEntryDto> entry : timeSeries.entrySet()) {
            String dateStr = entry.getKey();
            TimeSeriesEntryDto entryDto = entry.getValue();
            LocalDate parsedDate = LocalDate.parse(dateStr, formatter);

            // Eğer bu tarihte zaten bir kayıt varsa onu al, yoksa yeni oluştur.
            StockData stockData = existingByDate.getOrDefault(parsedDate, new StockData());
            if (stockData.getId() == null) { // Yeni kayıt ise sembol ve tarihi ata.
                stockData.setSymbol(normalizedSymbol);
                stockData.setDate(parsedDate);
            }
            // Fiyat ve hacim bilgilerini güncelle.
            stockData.setOpen(entryDto.getOpen());
            stockData.setHigh(entryDto.getHigh());
            stockData.setLow(entryDto.getLow());
            stockData.setClose(entryDto.getClose());
            stockData.setVolume(entryDto.getVolume());

            stockDataList.add(stockData);
        }

        // Toplu olarak kaydet, bu daha hızlı.
        stockDataRepository.saveAll(stockDataList);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<StockData> getStockDataBySymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return List.of();
        }
        // Sembolü büyük harfe çevirip boşlukları temizleyerek veritabanında ara.
        return stockDataRepository.findBySymbolOrderByDateDesc(symbol.trim().toUpperCase());
    }

    @Override
    public List<String> getAllSymbols() {
        // Veritabanındaki tüm farklı sembolleri getir.
        return stockDataRepository.findDistinctSymbols();
    }
}