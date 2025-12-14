package com.antigravity.trading.repository;

import com.antigravity.trading.domain.entity.ScheduledStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScheduledStockRepository extends JpaRepository<ScheduledStock, Long> {

    // 활성화된 종목만 조회
    List<ScheduledStock> findByEnabledTrue();

    // 종목코드로 조회
    Optional<ScheduledStock> findBySymbol(String symbol);

    // 종목코드 존재 여부 확인
    boolean existsBySymbol(String symbol);
}
