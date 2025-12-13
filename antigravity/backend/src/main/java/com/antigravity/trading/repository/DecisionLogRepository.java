package com.antigravity.trading.repository;

import com.antigravity.trading.domain.entity.DecisionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DecisionLogRepository extends JpaRepository<DecisionLog, Long> {
    List<DecisionLog> findByTraceId(String traceId);

    List<DecisionLog> findByBacktestRunId(Long backtestRunId);
}
