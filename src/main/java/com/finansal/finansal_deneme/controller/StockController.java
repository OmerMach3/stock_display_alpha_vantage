package com.finansal.finansal_deneme.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finansal.finansal_deneme.model.StockData;
import com.finansal.finansal_deneme.service.StockService;
import com.finansal.finansal_deneme.model.StockDailyData;

import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/stocks")
@Validated // IoC-managed validation at the API boundary; avoids manual checks and standardizes errors
public class StockController {
    private static final Logger log = LoggerFactory.getLogger(StockController.class);
    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    // Fetch all data for a given stock
    @GetMapping("/{symbol}")
    public ResponseEntity<List<StockData>> getStock(@PathVariable @NotBlank String symbol) {
        List<StockData> stockDataList = stockService.getStockDataBySymbol(symbol);
        if (stockDataList.isEmpty()) {
            return ResponseEntity.notFound().build(); // Return 404 when no data exists
        }
        return ResponseEntity.ok(stockDataList);
    }

    // Fetch all distinct stock symbols in the database
    @GetMapping("/symbols")
    public ResponseEntity<List<String>> getAllSymbols() {
        List<String> symbols = stockService.getAllSymbols();
        return ResponseEntity.ok(symbols);
    }

    // Fetch daily data (last 1000 days)
    @GetMapping("/{symbol}/daily")
    public ResponseEntity<List<StockDailyData>> getDaily(@PathVariable @NotBlank String symbol) {
        List<StockDailyData> list = stockService.getDailyStockDataBySymbol(symbol);
        if (list.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(list);
    }


    @PostMapping("/async/syncDaily/{symbol}")
    public CompletableFuture<ResponseEntity<String>> syncDailyAsync(@PathVariable @NotBlank String symbol) {
        log.info("Async daily sync request received for symbol: {}", symbol);
        
        return stockService.syncDailyStockDataAsync(symbol)
            .thenApply(result -> {
                log.info("Async daily sync completed for: {}", symbol);
                return ResponseEntity.ok("Async günlük veri senkronizasyonu tamamlandı: " + symbol);
            })
            .exceptionally(throwable -> {
                log.error("Async daily sync failed for {}: {}", symbol, throwable.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Async günlük sync hata: " + symbol + " - " + throwable.getMessage());
            });
    }

    // ASYNC monthly sync - responds immediately and runs in the background
    @PostMapping("/async/syncMonthly/{symbol}")
    public CompletableFuture<ResponseEntity<String>> syncMonthlyAsync(@PathVariable @NotBlank String symbol) {
        log.info("Async monthly sync request received for symbol: {}", symbol);
        
        return stockService.syncMonthlyStockDataAsync(symbol)
            .thenApply(result -> {
                log.info("Async monthly sync completed for: {}", symbol);
                return ResponseEntity.ok("Async aylık veri senkronizasyonu tamamlandı: " + symbol);
            })
            .exceptionally(throwable -> {
                log.error("Async monthly sync failed for {}: {}", symbol, throwable.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Async aylık sync hata: " + symbol + " - " + throwable.getMessage());
            });
    }

    // Intraday endpoint removed per requirement; monthly and daily endpoints remain
}
