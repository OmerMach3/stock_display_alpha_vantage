package com.finansal.finansal_deneme.repository;

import com.finansal.finansal_deneme.model.StockData;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StockDataRepository extends JpaRepository<StockData, Long> {

	List<StockData> findBySymbolOrderByDateDesc(String symbol);

	List<StockData> findBySymbolAndDateIn(String symbol, Collection<LocalDate> dates);

	@Query("SELECT DISTINCT s.symbol FROM StockData s")
	List<String> findDistinctSymbols();
	
	// PERFORMANCE OPTIMIZATION: Get only existing dates, not full entities
	@Query("SELECT s.date FROM StockData s WHERE s.symbol = :symbol AND s.date IN :dates")
	Set<LocalDate> findExistingDatesBySymbolAndDates(@Param("symbol") String symbol, @Param("dates") Collection<LocalDate> dates);
	@Query("SELECT MAX(s.date) FROM StockData s WHERE s.symbol = :symbol")
    Optional<LocalDate> findLatestDateBySymbol(@Param("symbol") String symbol);
}
