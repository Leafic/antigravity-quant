package com.antigravity.trading.repository;

import com.antigravity.trading.domain.entity.AutoTradeSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AutoTradeScheduleRepository extends JpaRepository<AutoTradeSchedule, Long> {
    List<AutoTradeSchedule> findByIsActiveTrue();
    Optional<AutoTradeSchedule> findBySymbolAndScheduleTime(String symbol, String scheduleTime);
}
