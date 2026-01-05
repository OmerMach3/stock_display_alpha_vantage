package com.finansal.finansal_deneme.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


import jakarta.persistence.EntityManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;


import com.finansal.finansal_deneme.dto.external.AlphaVantageDailyResponseDto;
import com.finansal.finansal_deneme.dto.external.TimeSeriesEntryDto;
import com.finansal.finansal_deneme.integration.FinancialDataProvider;
import com.finansal.finansal_deneme.model.StockData;
import com.finansal.finansal_deneme.model.StockDailyData;
import com.finansal.finansal_deneme.repository.StockDataRepository;
import com.finansal.finansal_deneme.repository.StockDailyDataRepository;
import com.finansal.finansal_deneme.service.StockService;
import com.finansal.finansal_deneme.util.UtilityMethods;

// Service class handling core stock operations
@Service
@Validated // Enables method parameter validation such as @NotBlank on service methods (IoC-friendly input validation)
public class StockServiceImpl implements StockService {
    private final StockDataRepository stockDataRepository;
    private final FinancialDataProvider financialDataProvider;
    private final StockDailyDataRepository stockDailyDataRepository;
    private final EntityManagerFactory entityManagerFactory;
    private final ApplicationContext applicationContext;

    private static final Logger log = LoggerFactory.getLogger(StockServiceImpl.class);

    // Constructor - Spring injects the needed dependencies for us
    public StockServiceImpl(StockDataRepository stockDataRepository,
                            FinancialDataProvider financialDataProvider,
                            StockDailyDataRepository stockDailyDataRepository,
                            EntityManagerFactory entityManagerFactory,
                            ApplicationContext applicationContext) {
        this.stockDataRepository = stockDataRepository;
        this.financialDataProvider = financialDataProvider;
        this.stockDailyDataRepository = stockDailyDataRepository;
        this.entityManagerFactory = entityManagerFactory;
        this.applicationContext = applicationContext;
    }

    /**
     * Get the proxied instance of this service to ensure Spring AOP works correctly
     * for @Transactional, @Cacheable, etc. when calling methods from within the service.
     */
    private StockService getProxiedSelf() {
        return applicationContext.getBean(StockService.class);
    }

    // ===============================================
    // SYNCHRONOUS BUSINESS LOGIC METHODS
    // Used by DataSyncScheduler (which handles @Async) and API endpoints
    // DataSyncScheduler controls the timing, these methods do the work
    // ===============================================

@Override
@Transactional
@CacheEvict(cacheNames = {"stocksBySymbol", "symbols"}, allEntries = true)
public void syncMonthlyStockData(String symbol) {
    String normalizedSymbol = symbol.trim().toUpperCase();
    log.info("Aylık senkronizasyon başlatılıyor: {}", normalizedSymbol);
    
    try {
        financialDataProvider.fetchMonthlyStockData(normalizedSymbol)
            .filter(response -> response.getMonthlyTimeSeries() != null && !response.getMonthlyTimeSeries().isEmpty())
            .ifPresentOrElse(
                response -> {
                    saveMonthlyTimeSeriesData(normalizedSymbol, response.getMonthlyTimeSeries());
                    log.info("Aylık senkronizasyon tamamlandı: {} ({} kayıt)", 
                             normalizedSymbol, response.getMonthlyTimeSeries().size());
                },
                () -> log.warn("Aylık veri alınamadı veya boş: {}", normalizedSymbol)
            );
            
    } catch (Exception e) {
        log.error("Aylık senkronizasyon hatası ({}): {}", normalizedSymbol, e.getMessage());
        throw e;
    }
}
@Override
@Transactional
@CacheEvict(cacheNames = {"stocksBySymbol", "symbols"}, allEntries = true)
public void syncDailyStockData(String symbol) {
    String normalizedSymbol = symbol.trim().toUpperCase();
    log.info("Günlük senkronizasyon başlatılıyor: {}", normalizedSymbol);
    
    try {
        // 1. Pull data from the API
        AlphaVantageDailyResponseDto response = financialDataProvider.fetchDailyStockData(normalizedSymbol)
            .orElseThrow(() -> new RuntimeException("API'den veri alınamadı: " + normalizedSymbol));

        Map<String, TimeSeriesEntryDto> series = response.getDailyTimeSeries();
        
        if (series != null && !series.isEmpty()) {
            // 2. Pass straight to the save method
            // Note: the isAfter(lastDate) filter inside saveDailyTimeSeriesData already removes duplicates and noise, so no need to prune here.
            saveDailyTimeSeriesData(normalizedSymbol, series);
            
            log.info("Günlük senkronizasyon başarıyla tamamlandı: {}", normalizedSymbol);
        }
        
    } catch (Exception e) {
        log.error("Günlük senkronizasyon hatası ({}): {}", normalizedSymbol, e.getMessage());
        throw e;
    }
}

  @Override
@Transactional
public void saveMonthlyTimeSeriesData(String symbol, Map<String, TimeSeriesEntryDto> timeSeries) {
    if (timeSeries == null || timeSeries.isEmpty()) return;

    String normalizedSymbol = symbol.trim().toUpperCase();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Step 1: find the latest month in the database (single DB call)
    // Also add @Query("SELECT MAX(s.date) FROM StockData s WHERE s.symbol = :symbol") to stockDataRepository
    LocalDate lastStoredMonth = stockDataRepository.findLatestDateBySymbol(normalizedSymbol)
            .orElse(LocalDate.of(1900, 1, 1));

    // Step 2: filter and map to entities
    List<StockData> stockDataList = timeSeries.entrySet().stream()
        .map(entry -> {
            LocalDate date = LocalDate.parse(entry.getKey(), formatter);
            return new AbstractMap.SimpleEntry<>(date, entry.getValue());
        })
        // Only take months after the latest stored one
        .filter(entry -> entry.getKey().isAfter(lastStoredMonth))
        .map(entry -> {
            StockData stockData = new StockData();
            stockData.setSymbol(normalizedSymbol);
            stockData.setDate(entry.getKey());
            stockData.setOpen(entry.getValue().getOpen());
            stockData.setHigh(entry.getValue().getHigh());
            stockData.setLow(entry.getValue().getLow());
            stockData.setClose(entry.getValue().getClose());
            stockData.setVolume(entry.getValue().getVolume());
            return stockData;
        })
        .collect(Collectors.toList());

    // Step 3: batch save
    if (!stockDataList.isEmpty()) {
        UtilityMethods.saveInBatches(stockDataList, entityManagerFactory);
        log.info("{} için {} yeni aylık veri eklendi.", normalizedSymbol, stockDataList.size());
    }
}



    @Transactional
public void saveDailyTimeSeriesData(String symbol, Map<String, TimeSeriesEntryDto> timeSeries) {
if (timeSeries == null || timeSeries.isEmpty()) return;

    String normalizedSymbol = symbol.trim().toUpperCase();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Get the most recent date in the DB; if none, fall back to a very old date (MIN).
    LocalDate lastStoredDate = stockDailyDataRepository.findLatestDateBySymbol(normalizedSymbol)
            .orElse(LocalDate.MIN);

    // Only keep entries after that date
    List<StockDailyData> persistList = timeSeries.entrySet().stream()
            .map(entry -> {
                LocalDate date = LocalDate.parse(entry.getKey(), formatter);
                return new AbstractMap.SimpleEntry<>(date, entry.getValue());
            })
            // Critical: accept only dates newer than the last stored one
            .filter(entry -> entry.getKey().isAfter(lastStoredDate)) 
            .map(entry -> {
                StockDailyData item = new StockDailyData();
                item.setSymbol(normalizedSymbol);
                item.setDate(entry.getKey());
                item.setOpen(entry.getValue().getOpen());
                item.setHigh(entry.getValue().getHigh());
                item.setLow(entry.getValue().getLow());
                item.setClose(entry.getValue().getClose());
                item.setVolume(entry.getValue().getVolume());
                return item;
            })
            .collect(Collectors.toList());

    if (!persistList.isEmpty()) {
        UtilityMethods.saveDailyInBatchesOptimized(persistList, stockDailyDataRepository, log);
        log.info("{} için {} yeni kayıt eklendi.", normalizedSymbol, persistList.size());
    }

}

    // ASYNC MONTHLY SYNC - returns a CompletableFuture
    // RECOMMENDED: API endpoints should call this method
    // WARNING: AlphaVantage 5 calls/minute limit - use responsibly
  @Override
@Async("taskExecutor")
public CompletableFuture<Void> syncMonthlyStockDataAsync(String symbol) {
    try {
        log.info("API call being made to AlphaVantage for monthly data: {} (Rate limit: 5 calls/minute)", symbol);
        
        getProxiedSelf().syncMonthlyStockData(symbol);
        
        log.info("Async monthly sync completed successfully for: {}", symbol);
        return CompletableFuture.completedFuture(null);
    } catch (Exception e) {
        log.error("Async monthly sync failed for {}: {}", symbol, e.getMessage(), e);
        return CompletableFuture.failedFuture(e);
    }
}
    // ASYNC DAILY SYNC - returns a CompletableFuture
// RECOMMENDED: API endpoints should call this method
// WARNING: AlphaVantage 5 calls/minute limit - use responsibly
@Override
@Async("taskExecutor")
public CompletableFuture<Void> syncDailyStockDataAsync(String symbol) {
    try {
        log.info("API call being made to AlphaVantage for daily data: {} (Rate limit: 5 calls/minute)", symbol);
        
        // Call the public, proxied method to ensure annotations are applied
        getProxiedSelf().syncDailyStockData(symbol);
        
        log.info("Async daily sync completed successfully for: {}", symbol);
        return CompletableFuture.completedFuture(null);
    } catch (Exception e) {
        log.error("Async daily sync failed for {}: {}", symbol, e.getMessage(), e);
        return CompletableFuture.failedFuture(e);
    }
}

    // DATA READ METHODS - cached and optimized

    // GET MONTHLY DATA - cached and fast
    @Override
    @Transactional(readOnly = true) // Sadece okuma - performans artışı
    @Cacheable(cacheNames = "stocksBySymbol", key = "#symbol.trim().toUpperCase()") // Cache'de tut - tekrar okumaya gerek yok
    public List<StockData> getStockDataBySymbol(String symbol) {
        // Normalize the symbol and fetch from the database, sorted newest to oldest
        return stockDataRepository.findBySymbolOrderByDateDesc(symbol.trim().toUpperCase());
    }

    @Override
    @Transactional(readOnly = true) // Read-only transactional context improves performance and expresses intent
    @Cacheable(cacheNames = "symbols") // Cache distinct symbols list
    public List<String> getAllSymbols() {
        // Fetch all distinct symbols from the database
        return stockDataRepository.findDistinctSymbols();
    }

    // GET DAILY DATA - fetch daily records for a given symbol
    @Override
    @Transactional(readOnly = true) // Sadece okuma - güvenli ve hızlı
    public List<StockDailyData> getDailyStockDataBySymbol(String symbol) {
        // Clean the symbol, uppercase it, and fetch daily data sorted newest to oldest
        return stockDailyDataRepository.findBySymbolOrderByDateDesc(symbol.trim().toUpperCase());
    }
  

}