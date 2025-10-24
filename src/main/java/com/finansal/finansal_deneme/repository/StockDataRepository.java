package com.finansal.finansal_deneme.repository;

import com.finansal.finansal_deneme.model.StockData;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface StockDataRepository extends JpaRepository<StockData, Long> {

	List<StockData> findBySymbolOrderByDateDesc(String symbol);

	List<StockData> findBySymbolAndDateIn(String symbol, Collection<LocalDate> dates);

	@Query("SELECT DISTINCT s.symbol FROM StockData s")
	List<String> findDistinctSymbols();
}
