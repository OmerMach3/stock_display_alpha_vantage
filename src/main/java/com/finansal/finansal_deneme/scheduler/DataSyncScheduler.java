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
    
    // Periyodik olarak veri çekilecek hisselerin listesi.
    // Alpha Vantage API limiti (5 istek/dakika) nedeniyle liste kısa tutulmuştur.
    private final List<String> stock_list = List.of(
        "AAPL","IBM","MSFT","GOOGL","AMZN","TSLA","META","NVDA","JPM","V",
        "DIS","NFLX","ADBE","PYPL","INTC","CSCO","ORCL","CRM","UBER","LYFT","CEG"
    );
    
    // Günlük veri çekimi için indeks sayacı
    private final AtomicInteger dailyStockIndex = new AtomicInteger(0);
    // Aylık veri çekimi için indeks sayacı
    private final AtomicInteger monthlyStockIndex = new AtomicInteger(0);
    // Günlük veri çekimi tamamlandı mı kontrolü
    private final AtomicBoolean dailyDataCompleted = new AtomicBoolean(false);

    public DataSyncScheduler(StockService stockService) {
        this.stockService = stockService;
    }

    // GÜNLÜK VERİ ÇEKME: Her 20 saniyede bir çalışır.
    // initialDelay = 5000: Uygulama başladıktan 5 saniye sonra ilk isteği atar.
    // Önce tüm hisselerin günlük verilerini çeker
    @Scheduled(fixedRate = 20000, initialDelay = 5000)
    public void syncNextStockDaily() {

        if (stock_list.isEmpty()) {
            return; // Hisse listesi boşsa bir şey yapma.
        }

        // Eğer günlük veri çekimi tamamlandıysa, bu metodu çalıştırma
        if (dailyDataCompleted.get()) {
            return;
        }

        // Atomik olarak mevcut indeksi alıp bir sonrakine güncelle.
        int currentIndex = dailyStockIndex.getAndUpdate(i -> (i + 1) % stock_list.size());
        String symbol = stock_list.get(currentIndex);

        log.info("Günlük veri çekiliyor: {} ({}/{})", symbol, currentIndex + 1, stock_list.size());

        try {
            // Günlük veri senkronizasyonu
            stockService.syncDailyStockData(symbol);
            
            // Eğer tüm hisseler için günlük veri çekimi tamamlandıysa işaretleme yap
            if (currentIndex == stock_list.size() - 1) {
                dailyDataCompleted.set(true);
                log.info("Tüm hisselerin günlük veri çekimi tamamlandı. Aylık veri çekimine geçiliyor...");
            }
        } catch (Exception e) {
            log.error("Günlük veri çekiminde {} için hata oluştu: {}", symbol, e.getMessage(), e);
        }
    }

    // AYLIK VERİ ÇEKME: Her 25 saniyede bir çalışır.
    // Günlük veri çekimi tamamlandıktan sonra başlar
    @Scheduled(fixedRate = 25000, initialDelay = 10000)
    public void syncNextStockMonthly() {

        if (stock_list.isEmpty()) {
            return; // Hisse listesi boşsa bir şey yapma.
        }

        // Günlük veri çekimi tamamlanmadıysa aylık veri çekimini başlatma
        if (!dailyDataCompleted.get()) {
            return;
        }

        // Atomik olarak mevcut indeksi alıp bir sonrakine güncelle.
        int currentIndex = monthlyStockIndex.getAndUpdate(i -> (i + 1) % stock_list.size());
        String symbol = stock_list.get(currentIndex);

        log.info("Aylık veri çekiliyor: {} ({}/{})", symbol, currentIndex + 1, stock_list.size());

        try {
            // Aylık veri senkronizasyonu
            stockService.syncMonthlyStockData(symbol);
        } catch (Exception e) {
            log.error("Aylık veri çekiminde {} için hata oluştu: {}", symbol, e.getMessage(), e);
        }
    }
}