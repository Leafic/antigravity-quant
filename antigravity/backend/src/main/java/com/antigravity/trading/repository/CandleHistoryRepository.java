package com.antigravity.trading.repository;

import com.antigravity.trading.domain.entity.CandleHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CandleHistoryRepository extends JpaRepository<CandleHistory, Long> {
    List<CandleHistory> findBySymbolAndTimeBetween(String symbol, LocalDateTime start, LocalDateTime end);

    @Query("SELECT MIN(c.time) FROM CandleHistory c WHERE c.symbol = :symbol")
    LocalDateTime findMinTimeBySymbol(@Param("symbol") String symbol);

    @Query("SELECT MAX(c.time) FROM CandleHistory c WHERE c.symbol = :symbol")
    LocalDateTime findMaxTimeBySymbol(@Param("symbol") String symbol);
}
