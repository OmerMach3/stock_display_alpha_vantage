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

// Use fully-qualified name for Reactor's Retry to avoid name clash with Resilience4j's @Retry annotation

@Service
public class AlphaVantageProvider implements FinancialDataProvider {
    private static final Logger log = LoggerFactory.getLogger(AlphaVantageProvider.class);

    private final WebClient webClient;
    private final String apiKey;

    public AlphaVantageProvider(WebClient.Builder webClientBuilder,
                FinancialApiProperties apiProperties) {
        // Bazen sistemler IPv6'yı önceliklendirir ve bu da bağlantı sorunlarına yol açabilir.
        // Bu ayar, Java'ya ağ bağlantıları için IPv4'ü tercih etmesini söyler.
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.net.preferIPv6Addresses", "false");

        log.debug("IPv4 kullanımı zorlandı!");

    // Externalized configuration via IoC-managed properties makes testing and environment switching easier
    this.apiKey = apiProperties.getKey();

        this.webClient = webClientBuilder
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer
                                .defaultCodecs()
                                .maxInMemorySize(16 * 1024 * 1024)) // Gelen yanıtın max boyutunu 16MB'a çıkar.
                        .build())
        .baseUrl(apiProperties.getUrl())
                .build();
    }

    private <T> Optional<T> fetchApiData(String function, String symbol, Class<T> responseType) {
        log.debug("AlphaVantageProvider request. function={}, symbol={}", function, symbol);

        try {
            T response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("function", function)
                            .queryParam("symbol", symbol)
                            .queryParam("apikey", apiKey)
                            .queryParam("outputsize", "full") // Tüm veriyi çekmek için.
                            .build())
                    .retrieve()
                    .bodyToMono(responseType)
                    .timeout(Duration.ofSeconds(30))
                    .retryWhen(reactor.util.retry.Retry.backoff(2, Duration.ofSeconds(2)))
                    .doOnError(error -> {
                        log.error("WebClient error while calling {}: {}", function, error.getMessage(), error);
                    })
                    .block(); // Asenkron işlemi senkron olarak bekletir ve sonucu alır.

            if (response == null) {
                log.warn("{} API'den yanıt alındı ama DTO 'null' geldi. Muhtemel API limiti veya bozuk JSON.", function);
                return Optional.empty();
            }

            // API limitine takılınca gelen "Information" veya "Note" içeren yanıtları kontrol et.
            if (response instanceof AlphaVantageIntradayResponseDto && ((AlphaVantageIntradayResponseDto) response).getMetaData() == null) {
                log.warn("Intraday DTO'sunda MetaData 'null'. API Limiti olabilir.");
                return Optional.empty();
            }
            if (response instanceof AlphaVantageMonthlyResponseDto && ((AlphaVantageMonthlyResponseDto) response).getMetaData() == null) {
                log.warn("Monthly DTO'sunda MetaData 'null'. API Limiti olabilir.");
                return Optional.empty();
            }
            if (response instanceof AlphaVantageDailyResponseDto && ((AlphaVantageDailyResponseDto) response).getMetaData() == null) {
                log.warn("Daily DTO'sunda MetaData 'null'. API Limiti olabilir.");
                return Optional.empty();
            }

            log.debug("{} API'den yanıt başarıyla alındı ve DTO'ya dönüştürüldü.", function);
            return Optional.of(response);

        } catch (Exception e) {
            log.error("AlphaVantageProvider critical error for {}: {}", function, e.getMessage(), e);
            return Optional.empty();
        }
    }


    @Retry(name = "alphaVantage") // IoC-managed retry policy (configurable via properties)
    @CircuitBreaker(name = "alphaVantage") // Opens circuit on repeated failures to protect downstream
    public Optional<AlphaVantageIntradayResponseDto> fetchIntradayStockData(String symbol) {
        log.debug("AlphaVantageProvider intraday request. symbol={}", symbol);
        try {
            AlphaVantageIntradayResponseDto response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("function", "TIME_SERIES_INTRADAY")
                            .queryParam("symbol", symbol)
                            .queryParam("interval", "5min")
                            .queryParam("outputsize", "full")
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(AlphaVantageIntradayResponseDto.class)
                    .timeout(Duration.ofSeconds(30))
                    .retryWhen(reactor.util.retry.Retry.backoff(2, Duration.ofSeconds(2)))
                    .doOnError(error -> log.error("WebClient error while calling intraday: {}", error.getMessage(), error))
                    .block();

            if (response == null || response.getMetaData() == null) {
                log.warn("Intraday yanıtı boş veya MetaData null olabilir (API limiti)");
                return Optional.empty();
            }

            return Optional.of(response);
        } catch (Exception e) {
            log.error("AlphaVantageProvider critical error for intraday: {}", e.getMessage(), e);
            return Optional.empty();
        }
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
