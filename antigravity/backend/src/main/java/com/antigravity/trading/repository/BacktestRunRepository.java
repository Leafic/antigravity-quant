package com.antigravity.trading.repository;

import com.antigravity.trading.domain.entity.BacktestRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BacktestRunRepository extends JpaRepository<BacktestRun, Long> {
}
