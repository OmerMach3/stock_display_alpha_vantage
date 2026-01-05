package com.finansal.finansal_deneme.integration;

import java.util.Optional;

import com.finansal.finansal_deneme.dto.external.AlphaVantageIntradayResponseDto;
import com.finansal.finansal_deneme.dto.external.AlphaVantageDailyResponseDto;
import com.finansal.finansal_deneme.dto.external.AlphaVantageMonthlyResponseDto;

public interface FinancialDataProvider {
    // Using Optional<T> avoids NullPointerExceptions when data is missing.
    Optional<AlphaVantageIntradayResponseDto> fetchIntradayStockData(String symbol);
    
    // Method for monthly data
    Optional<AlphaVantageMonthlyResponseDto> fetchMonthlyStockData(String symbol);

    // Method for daily data
    Optional<AlphaVantageDailyResponseDto> fetchDailyStockData(String symbol);
}
