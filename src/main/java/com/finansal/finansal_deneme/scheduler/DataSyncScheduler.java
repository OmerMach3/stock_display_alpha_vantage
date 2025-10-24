package com.finansal.finansal_deneme.scheduler;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
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
    
    // Sıradaki hissenin indeksini tutan, thread-safe bir sayaç.
    private final AtomicInteger stockIndex = new AtomicInteger(0);

    public DataSyncScheduler(StockService stockService) {
        this.stockService = stockService;
    }

    // AYLIK VERİ ÇEKME: Her 20 saniyede bir çalışır.
    // initialDelay = 5000: Uygulama başladıktan 5 saniye sonra ilk isteği atar.
    @Scheduled(fixedRate = 20000, initialDelay = 5000)
    public void syncNextStockMonthly() {

        if (stock_list.isEmpty()) {
            return; // Hisse listesi boşsa bir şey yapma.
        }

        // Atomik olarak mevcut indeksi alıp bir sonrakine güncelle.
        // Böylece aynı anda birden fazla scheduler çalıştırılsa bile
        // her çağrı farklı bir hisseyi işleyip, atlama/tekrar sorunlarını önleriz.
        int currentIndex = stockIndex.getAndUpdate(i -> (i + 1) % stock_list.size());
        String symbol = stock_list.get(currentIndex);

        log.info("Aylık veri çekiliyor: {} ({}/{})", symbol, currentIndex + 1, stock_list.size());

        try {
            // Servisi asenkron ya da senkron çağır; hata olursa yakalayıp logla.
            stockService.syncMonthlyStockData(symbol);
        } catch (Exception e) {
            log.error("Zamanlayıcıda {} için hata oluştu: {}", symbol, e.getMessage(), e);
            // İndeksi önceden atomik olarak artırdığımız için burada ekstra işlem yapmaya gerek yok.
        }
    }
}