package com.finansal.finansal_deneme.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finansal.finansal_deneme.model.StockData;
import com.finansal.finansal_deneme.service.StockService;

@RestController
@RequestMapping("/api/stocks")
public class StockController {
    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    // Belirli bir hisse senedinin tüm verilerini getirir.
    @GetMapping("/{symbol}")
    public ResponseEntity<List<StockData>> getStock(@PathVariable String symbol) {
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

    // Belirli bir hisse senedi için veri senkronizasyonunu manuel olarak tetikler.
    @PostMapping("/sync/{symbol}")
    public ResponseEntity<String> syncStockData(@PathVariable String symbol) {
        try {
            stockService.syncStockData(symbol);
            return ResponseEntity.ok("Veri senkronizasyonu başarılı: " + symbol);
        } catch (Exception e) {
            // Hata durumunda sunucu hatası (500) ve hata mesajını döner.
            return ResponseEntity.internalServerError()
                    .body("Veri senkronizasyonu sırasında bir hata oluştu: " + e.getMessage());
        }
    }
}
