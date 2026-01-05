package com.finansal.finansal_deneme.scheduler;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.finansal.finansal_deneme.service.StockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Component
public class DataSyncScheduler {
    private final StockService stockService;
    private static final Logger log = LoggerFactory.getLogger(DataSyncScheduler.class);
    //Add any ticker you want to sync here
    private final List<String> stock_list = List.of(
        "AAPL","IBM","MSFT","GOOGL","AMZN","TSLA","META","NVDA","JPM","V",
        "DIS","NFLX","ADBE","PYPL","INTC","CSCO","ORCL","CRM","UBER","LYFT","CEG"
    );
    
    private int dailyStockIndex = 0;
    private int monthlyStockIndex = 0;
    private boolean isDailyCycleActive = true; // Start with daily data first

    public DataSyncScheduler(StockService stockService) {
        this.stockService = stockService;
    }

    @Scheduled(fixedRate = 20000, initialDelay = 5000)
    public void syncNextStock() {
        if (stock_list.isEmpty()) return;

        if (isDailyCycleActive) {
            runDailySync();
        } else {
            runMonthlySync();
        }
    }

    private void runDailySync() {
        String symbol = stock_list.get(dailyStockIndex);
        log.info("[GÜNLÜK DÖNGÜ] İşleniyor: {} ({}/{})", symbol, dailyStockIndex + 1, stock_list.size());

        try {
            stockService.syncDailyStockData(symbol);
            dailyStockIndex++;

            // End of the list?
            if (dailyStockIndex >= stock_list.size()) {
                dailyStockIndex = 0; // Reset to the start
                isDailyCycleActive = false; // Daily run finished, switch to monthly cycle
                log.info(">>> Tüm hisselerin günlük verileri güncellendi. Aylık döngü başlıyor...");
            }
        } catch (Exception e) {
            log.error("Günlük veri hatası ({}): {}", symbol, e.getMessage());
            dailyStockIndex++; // Even on error, move to the next so the loop does not stall
        }
    }

    private void runMonthlySync() {
        String symbol = stock_list.get(monthlyStockIndex);
        log.info("[AYLIK DÖNGÜ] İşleniyor: {} ({}/{})", symbol, monthlyStockIndex + 1, stock_list.size());

        try {
            stockService.syncMonthlyStockData(symbol);
            monthlyStockIndex++;

            // End of the list?
            if (monthlyStockIndex >= stock_list.size()) {
                monthlyStockIndex = 0; // Reset to the start
                isDailyCycleActive = true; // Monthly run finished, back to daily cycle
                log.info(">>> Tüm hisselerin aylık verileri güncellendi. Günlük döngüye geri dönülüyor...");
            }
        } catch (Exception e) {
            log.error("Aylık veri hatası ({}): {}", symbol, e.getMessage());
            monthlyStockIndex++;
        }
    }
}