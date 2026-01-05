package com.finansal.finansal_deneme.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.validation.annotation.Validated;

import com.finansal.finansal_deneme.dto.external.TimeSeriesEntryDto;
import com.finansal.finansal_deneme.model.StockData;
import com.finansal.finansal_deneme.model.StockDailyData;

import jakarta.validation.constraints.NotBlank;

@Validated // Method validation should be declared at the API (interface) level
public interface StockService {
    
    /**
     * Pulls monthly data for a stock from the external API and saves it.
     * Runs asynchronously (non-blocking).
     * INTERNAL USE: for the scheduler and other internal operations.
     * For API calls, prefer syncMonthlyStockDataAsync().
     * @param symbol Stock symbol (e.g., "IBM").
     */
    void syncMonthlyStockData(@NotBlank(message = "Symbol must not be blank") String symbol);

    /**
     * Async monthly data sync - returns a CompletableFuture.
     * RECOMMENDED: API endpoints should use this method.
     * @param symbol Stock symbol
     * @return CompletableFuture<Void> - async operation result
     */
    CompletableFuture<Void> syncMonthlyStockDataAsync(@NotBlank(message = "Symbol must not be blank") String symbol);

    /**
     * Fetches all data for the given stock from the database.
     * @param symbol Stock symbol.
     * @return All StockData records for the symbol (sorted newest to oldest).
     */
    List<StockData> getStockDataBySymbol(@NotBlank(message = "Symbol must not be blank") String symbol);

    /**
     * Persists incoming time series data.
     * @param symbol Stock symbol.
     * @param timeSeries Map containing date and price info.
     */
    void saveMonthlyTimeSeriesData(@NotBlank(message = "Symbol must not be blank") String symbol,
                            Map<String, TimeSeriesEntryDto> timeSeries);

    /**
     * Fetches all distinct stock symbols stored in the database.
     * @return List of symbols.
     */
    List<String> getAllSymbols();

    // Daily data sync and access
    /**
     * INTERNAL USE: for the scheduler and other internal operations.
     * For API calls, prefer syncDailyStockDataAsync().
     */
    void syncDailyStockData(@NotBlank(message = "Symbol must not be blank") String symbol);

    /**
     * Async daily data sync - returns a CompletableFuture.
     * RECOMMENDED: API endpoints should use this method.
     * @param symbol Stock symbol
     * @return CompletableFuture<Void> - async operation result
     */
    CompletableFuture<Void> syncDailyStockDataAsync(@NotBlank(message = "Symbol must not be blank") String symbol);

    void saveDailyTimeSeriesData(@NotBlank(message = "Symbol must not be blank") String symbol,
                                 Map<String, TimeSeriesEntryDto> timeSeries);

    List<StockDailyData> getDailyStockDataBySymbol(@NotBlank(message = "Symbol must not be blank") String symbol);
}
