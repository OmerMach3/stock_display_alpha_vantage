package com.finansal.finansal_deneme.util;

import com.finansal.finansal_deneme.dto.external.TimeSeriesEntryDto;
import com.finansal.finansal_deneme.model.StockDailyData;
import com.finansal.finansal_deneme.repository.StockDailyDataRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Utility class for common helper methods.
 */
public final class UtilityMethods {

    private UtilityMethods() {
        // Private constructor to prevent instantiation.
    }
    @PersistenceContext
    private EntityManager entityManager;

    private static final int DEFAULT_BATCH_SIZE = 100;

    /**
     * Her türlü Entity listesini belirtilen batch boyutunda kaydeder.
     */


    /**
     * Saves a list of entities in batches.
     * @param entityList The list of entities to save.
     * @param entityManagerFactory The factory to create an EntityManager.
     * @param <T> The type of the entities.
     */
     public static <T> void saveInBatches(List<T> entityList, EntityManagerFactory entityManagerFactory) {
        if (entityList == null || entityList.isEmpty()) {
            return;
        }
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            for (int i = 0; i < entityList.size(); i++) {
                em.persist(entityList.get(i));
                if (i > 0 && i % 1000 == 0) {
                    em.flush();
                    em.clear();
                }
            }
            em.flush();
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Batch save failed", e);
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    /**
     * Filters and returns the most recent entries from a time series map.
     * @param series The map of date strings to time series entries.
     * @param limit The maximum number of recent entries to return.
     * @return A map containing only the most recent entries, sorted from new to old.
     */
    public static Map<String, TimeSeriesEntryDto> getRecentDates(Map<String, TimeSeriesEntryDto> series, int limit) {
        if (series == null || series.isEmpty()) {
            return new LinkedHashMap<>();
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return series.entrySet().stream()
                .sorted((a, b) -> LocalDate.parse(b.getKey(), formatter)
                        .compareTo(LocalDate.parse(a.getKey(), formatter)))
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }
    //  Get dates from the last 2 years (for validation)
public static List<LocalDate> getRecentDatesForValidation(Map<String, TimeSeriesEntryDto> timeSeries, 
            DateTimeFormatter formatter, 
            Clock clock) {
// Use the clock instead of LocalDate.now() to get today's date
        LocalDate today = LocalDate.now(clock);
        LocalDate twoYearsAgo = today.minusYears(2);
        
        return timeSeries.keySet().stream()
            .map(dateStr -> LocalDate.parse(dateStr, formatter))
            .filter(date -> !date.isBefore(twoYearsAgo)) 
            .sorted(Collections.reverseOrder())
            .collect(Collectors.toList());
}

//  OPTIMIZED SERIES FILTERING
private Map<String, TimeSeriesEntryDto> optimizeDailySeries(Map<String, TimeSeriesEntryDto> series) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    //  Filter and sort in one Stream pass
    return series.entrySet().stream()
        // 1. Combine date parsing with filtering
        .map(entry -> {
            try {
                LocalDate date = LocalDate.parse(entry.getKey(), formatter);
                return new AbstractMap.SimpleEntry<>(date, entry);
            } catch (Exception e) {
                return null; // Drop invalid dates
            }
        })
        .filter(Objects::nonNull) // Drop nulls
        // 2. Sort by date (newest to oldest)
        .sorted((a, b) -> b.getKey().compareTo(a.getKey()))
        // 3. Keep the last 1000 days
        .limit(1000)
        // 4. Convert back to the original format
        .collect(Collectors.toMap(
            entry -> entry.getValue().getKey(), // Original string key
            entry -> entry.getValue().getValue(),
            (e1, e2) -> e1,
            LinkedHashMap::new // Keep the order
        ));
}

// Helper: optimized batch save
public static void saveDailyInBatchesOptimized(List<StockDailyData> dataList, StockDailyDataRepository stockDailyDataRepository, Logger log) {
    if (dataList.isEmpty()) {
        return;
    }

    int batchSize = 100;
    for (int i = 0; i < dataList.size(); i += batchSize) {
        int end = Math.min(i + batchSize, dataList.size());
        List<StockDailyData> batch = dataList.subList(i, end);
        stockDailyDataRepository.saveAll(batch);
        
        if (log.isDebugEnabled()) {
            log.debug("Daily batch saved: {}-{} of {}", i + 1, end, dataList.size());
        }
    }
}
}
