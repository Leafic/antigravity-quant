package com.antigravity.trading.repository;

import com.antigravity.trading.domain.entity.TargetStock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TargetStockRepository extends JpaRepository<TargetStock, Long> {
    List<TargetStock> findByIsActiveTrue();
    boolean existsBySymbol(String symbol);
}
