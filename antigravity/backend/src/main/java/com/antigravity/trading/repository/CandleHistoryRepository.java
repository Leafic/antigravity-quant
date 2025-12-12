package com.antigravity.trading.repository;

import com.antigravity.trading.domain.entity.CandleHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface CandleHistoryRepository extends JpaRepository<CandleHistory, Long> {
    List<CandleHistory> findBySymbolAndTimeBetween(String symbol, LocalDateTime start, LocalDateTime end);
}
