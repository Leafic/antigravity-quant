package com.antigravity.trading.repository;

import com.antigravity.trading.domain.entity.TradeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TradeLogRepository extends JpaRepository<TradeLog, Long> {
    List<TradeLog> findBySymbol(String symbol);

    List<TradeLog> findAllByOrderByTimestampDesc();
}
