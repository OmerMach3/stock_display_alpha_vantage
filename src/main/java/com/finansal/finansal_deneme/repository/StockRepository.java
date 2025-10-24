package com.finansal.finansal_deneme.repository;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.finansal.finansal_deneme.model.Stock;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
    
    // Find by symbol
    List<Stock> findBySymbol(String symbol);
    
    Optional<Stock> findFirstBySymbolOrderByLastRefreshedDesc(String symbol);
    
    // Find by symbol and date
    Optional<Stock> findBySymbolAndLastRefreshed(String symbol, LocalDateTime lastRefreshed);
    
    // Find latest entry for a symbol
    @Query("SELECT s FROM Stock s WHERE s.symbol = :symbol ORDER BY s.lastRefreshed DESC")
    List<Stock> findLatestBySymbol(@Param("symbol") String symbol);
    
    // Find all entries for a symbol ordered by date
    @Query("SELECT s FROM Stock s WHERE s.symbol = :symbol ORDER BY s.lastRefreshed DESC")
    List<Stock> findBySymbolOrderByLastRefreshedDesc(@Param("symbol") String symbol);
}
