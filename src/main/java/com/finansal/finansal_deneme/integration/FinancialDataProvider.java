package com.finansal.finansal_deneme.integration;

import java.util.Optional;

import com.finansal.finansal_deneme.dto.external.AlphaVantageIntradayResponseDto;
import com.finansal.finansal_deneme.dto.external.AlphaVantageMonthlyResponseDto;

public interface FinancialDataProvider {
    // Optional<T> kullanımı, veri bulunamadığında NullPointerException yerine
    // güvenli bir yapı sunar.
    Optional<AlphaVantageIntradayResponseDto> fetchIntradayStockData(String symbol);
    
    // Monthly data için yeni method
    Optional<AlphaVantageMonthlyResponseDto> fetchMonthlyStockData(String symbol);
}
