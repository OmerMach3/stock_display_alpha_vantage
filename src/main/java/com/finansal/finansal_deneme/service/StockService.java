package com.finansal.finansal_deneme.service;

import java.util.List;
import java.util.Map;

import org.springframework.validation.annotation.Validated;

import com.finansal.finansal_deneme.dto.external.TimeSeriesEntryDto;
import com.finansal.finansal_deneme.model.StockData;
import com.finansal.finansal_deneme.model.StockDailyData;

import jakarta.validation.constraints.NotBlank;

@Validated // Method validation should be declared at the API (interface) level
public interface StockService {
    
    /**
     * Bir hisse senedinin aylık verilerini dış API'den çeker ve veritabanına kaydeder.
     * Bu işlem asenkron olarak (non-blocking) çalışır.
     * @param symbol Hisse senedi sembolü (örn: "IBM").
     */
    void syncMonthlyStockData(@NotBlank(message = "Symbol must not be blank") String symbol);

    /**
     * Belirtilen hisse senedinin tüm verilerini veritabanından çeker.
     * @param symbol Hisse senedi sembolü.
     * @return İlgili sembole ait tüm `StockData` kayıtları (en yeniden en eskiye sıralı).
     */
    List<StockData> getStockDataBySymbol(@NotBlank(message = "Symbol must not be blank") String symbol);

    /**
     * Gelen zaman serisi verisini veritabanına kaydeder.
     * @param symbol Hisse senedi sembolü.
     * @param timeSeries Tarih ve fiyat bilgilerini içeren map.
     */
    void saveMonthlyTimeSeriesData(@NotBlank(message = "Symbol must not be blank") String symbol,
                            Map<String, TimeSeriesEntryDto> timeSeries);

    /**
     * Veritabanında kayıtlı olan tüm farklı hisse senedi sembollerini getirir.
     * @return Sembollerin listesi.
     */
    List<String> getAllSymbols();

    // Günlük veri senkronizasyonu ve erişimi
    void syncDailyStockData(@NotBlank(message = "Symbol must not be blank") String symbol);

    void saveDailyTimeSeriesData(@NotBlank(message = "Symbol must not be blank") String symbol,
                                 Map<String, TimeSeriesEntryDto> timeSeries);

    List<StockDailyData> getDailyStockDataBySymbol(@NotBlank(message = "Symbol must not be blank") String symbol);
}
