package com.finansal.finansal_deneme.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    // Belirli bir hisse senedinin tüm verilerini getirir.
    @GetMapping("/{symbol}")
    public ResponseEntity<List<StockData>> getStock(@PathVariable @NotBlank String symbol) {
        List<StockData> stockDataList = stockService.getStockDataBySymbol(symbol);
        if (stockDataList.isEmpty()) {
            return ResponseEntity.notFound().build(); // Veri bulunamazsa 404 döner.
        }
        return ResponseEntity.ok(stockDataList);
    }

    // Veritabanında kayıtlı tüm farklı hisse senedi sembollerini getirir.
    @GetMapping("/symbols")
    public ResponseEntity<List<String>> getAllSymbols() {
        List<String> symbols = stockService.getAllSymbols();
        return ResponseEntity.ok(symbols);
    }

    // Günlük veriyi (son 1000 gün) getirir
    @GetMapping("/{symbol}/daily")
    public ResponseEntity<List<StockDailyData>> getDaily(@PathVariable @NotBlank String symbol) {
        List<StockDailyData> list = stockService.getDailyStockDataBySymbol(symbol);
        if (list.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(list);
    }

    // Günlük veri senkronizasyonunu tetikler (TIME_SERIES_DAILY -> 1000 gün)
    @PostMapping("/syncDaily/{symbol}")
    public ResponseEntity<String> syncDaily(@PathVariable @NotBlank String symbol) {
        stockService.syncDailyStockData(symbol);
        return ResponseEntity.ok("Günlük veri senkronizasyonu tetiklendi: " + symbol);
    }

    // Aylık veri senkronizasyonunu tetikler (TIME_SERIES_MONTHLY)
    @PostMapping("/syncMonthly/{symbol}")
    public ResponseEntity<String> syncMonthly(@PathVariable @NotBlank String symbol) {
        stockService.syncMonthlyStockData(symbol);
        return ResponseEntity.ok("Aylık veri senkronizasyonu tetiklendi: " + symbol);
    }

    // Intraday endpoint removed per requirement; monthly and daily endpoints remain
}
