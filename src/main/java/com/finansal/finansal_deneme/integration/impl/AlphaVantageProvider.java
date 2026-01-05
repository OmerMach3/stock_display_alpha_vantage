package com.finansal.finansal_deneme.integration.impl;

import java.time.Duration;
import java.util.Optional;

import com.finansal.finansal_deneme.config.FinancialApiProperties;
import com.finansal.finansal_deneme.dto.external.AlphaVantageIntradayResponseDto;
import com.finansal.finansal_deneme.dto.external.AlphaVantageDailyResponseDto;
import com.finansal.finansal_deneme.dto.external.AlphaVantageMonthlyResponseDto;
import com.finansal.finansal_deneme.integration.FinancialDataProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

// Use fully-qualified name for Reactor's Retry to avoid name clash with Resilience4j's @Retry annotation

@Service
public class AlphaVantageProvider implements FinancialDataProvider {
    private static final Logger log = LoggerFactory.getLogger(AlphaVantageProvider.class);
    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final String apiKey;

    public AlphaVantageProvider(WebClient.Builder webClientBuilder,
                FinancialApiProperties apiProperties,ObjectMapper objectMapper) {
        // Some systems prefer IPv6 first, which can trigger connection hiccups.
        // This setting nudges Java to favor IPv4 for network calls.
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.net.preferIPv6Addresses", "false");
        this.objectMapper = objectMapper;            
        log.debug("IPv4 kullanımı zorlandı!");

    // Externalized configuration via IoC-managed properties makes testing and environment switching easier
    this.apiKey = apiProperties.getKey();

        this.webClient = webClientBuilder
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer
                                .defaultCodecs()
                                .maxInMemorySize(16 * 1024 * 1024)) // Bump response buffer limit to 16MB.
                        .build())
        .baseUrl(apiProperties.getUrl())
                .build();
    }

private <T> Optional<T> fetchApiData(String function, String symbol, Class<T> responseType) {
    log.info("Alpha Vantage API çağrısı yapılıyor: function={}, symbol={}", function, symbol);

    try {
        // Step 1: make the API call once and grab it as a String.
        String rawResponse = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("function", function)
                        .queryParam("symbol", symbol)
                        .queryParam("apikey", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .retryWhen(reactor.util.retry.Retry.backoff(2, Duration.ofSeconds(2)))
                .block();

        if (rawResponse == null || rawResponse.isEmpty()) {
            log.error("API'den boş yanıt döndü: function={}, symbol={}", function, symbol);
            return Optional.empty();
        }

        // Step 2: check for error or info messages (rate limits, etc.).
        if (rawResponse.contains("Error Message") || rawResponse.contains("Note") || rawResponse.contains("Information")) {
            log.warn("Alpha Vantage Mesajı: {}", rawResponse);
            return Optional.empty();
        }

        // Step 3: turn the single String payload into the DTO with ObjectMapper.
        // objectMapper should live at class level and be built once.
        T response = objectMapper.readValue(rawResponse, responseType);

        // Step 4: check DTO-specific MetaData (could be shorter with reflection or a common interface).
        if (isMetaDataMissing(response)) {
            log.warn("{} yanıtında MetaData eksik. API limiti veya geçersiz sembol olabilir.", function);
            return Optional.empty();
        }

        return Optional.ofNullable(response);

    } catch (Exception e) {
        log.error("AlphaVantageProvider kritik hata ({}): {}", function, e.getMessage());
        return Optional.empty();
    }
}

// Helper: centralizes the repeated MetaData checks.
private boolean isMetaDataMissing(Object response) {
    if (response instanceof AlphaVantageIntradayResponseDto r) return r.getMetaData() == null;
    if (response instanceof AlphaVantageMonthlyResponseDto r) return r.getMetaData() == null;
    if (response instanceof AlphaVantageDailyResponseDto r) return r.getMetaData() == null;
    return false;
}

@Retry(name = "alphaVantage")
@CircuitBreaker(name = "alphaVantage")
public Optional<AlphaVantageIntradayResponseDto> fetchIntradayStockData(String symbol) {
    // Hand off the WebClient heavy lifting to the shared helper we already wrote.
    return fetchApiData("TIME_SERIES_INTRADAY", symbol, AlphaVantageIntradayResponseDto.class);
}

    @Override
    @Retry(name = "alphaVantage")
    @CircuitBreaker(name = "alphaVantage")
    public Optional<AlphaVantageMonthlyResponseDto> fetchMonthlyStockData(String symbol) {
        return fetchApiData("TIME_SERIES_MONTHLY", symbol, AlphaVantageMonthlyResponseDto.class);
    }

    @Override
    @Retry(name = "alphaVantage")
    @CircuitBreaker(name = "alphaVantage")
    public Optional<AlphaVantageDailyResponseDto> fetchDailyStockData(String symbol) {
        return fetchApiData("TIME_SERIES_DAILY", symbol, AlphaVantageDailyResponseDto.class);
    }
}
