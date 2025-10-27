package com.finansal.finansal_deneme.repository;

import com.finansal.finansal_deneme.model.StockDailyData;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface StockDailyDataRepository extends JpaRepository<StockDailyData, Long> {

    List<StockDailyData> findBySymbolOrderByDateDesc(String symbol);

    List<StockDailyData> findBySymbolAndDateIn(String symbol, Collection<LocalDate> dates);

    @Query("SELECT DISTINCT s.symbol FROM StockDailyData s")
    List<String> findDistinctSymbols();
}
