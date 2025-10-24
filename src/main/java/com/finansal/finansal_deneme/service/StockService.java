package com.finansal.finansal_deneme.service;

import java.util.List;
import java.util.Map;

import com.finansal.finansal_deneme.dto.external.TimeSeriesEntryDto;
import com.finansal.finansal_deneme.model.StockData;

public interface StockService {
    
    /**
     * Veritabanındaki bir hisse senedinin verilerini dış API'den gelenlerle günceller.
     * @param symbol Güncellenecek hisse senedinin sembolü (örn: "IBM").
     */
    void syncStockData(String symbol);

    /**
     * Bir hisse senedinin aylık verilerini dış API'den çeker ve veritabanına kaydeder.
     * Bu işlem asenkron olarak (non-blocking) çalışır.
     * @param symbol Hisse senedi sembolü (örn: "IBM").
     */
    void syncMonthlyStockData(String symbol);

    /**
     * Belirtilen hisse senedinin tüm verilerini veritabanından çeker.
     * @param symbol Hisse senedi sembolü.
     * @return İlgili sembole ait tüm `StockData` kayıtları (en yeniden en eskiye sıralı).
     */
    List<StockData> getStockDataBySymbol(String symbol);

    /**
     * Gelen zaman serisi verisini veritabanına kaydeder.
     * @param symbol Hisse senedi sembolü.
     * @param timeSeries Tarih ve fiyat bilgilerini içeren map.
     */
    void saveTimeSeriesData(String symbol, Map<String, TimeSeriesEntryDto> timeSeries);

    /**
     * Veritabanında kayıtlı olan tüm farklı hisse senedi sembollerini getirir.
     * @return Sembollerin listesi.
     */
    List<String> getAllSymbols();
}
