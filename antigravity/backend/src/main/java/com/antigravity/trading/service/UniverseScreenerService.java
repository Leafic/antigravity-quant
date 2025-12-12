package com.antigravity.trading.service;

import com.antigravity.trading.domain.entity.TargetStock;
import com.antigravity.trading.repository.TargetStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Phase 1: Universe Screening Service
 * 매일 밤 실행되어 거래량, 시가총액 등의 기준으로 다음 날 거래할 유니버스를 선정합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UniverseScreenerService {

    private final TargetStockRepository targetStockRepository;

    /**
     * 메인 스크리닝 배치 작업
     * 매일 오전 8시 30분에 실행 (장 시작 전)
     */
    @Scheduled(cron = "0 30 8 * * *") // 매일 08:30:00 실행
    @Transactional
    public void executeScreening() {
        log.info("Phase 1: Starting Universe Screening...");

        // 1. Deactivate all existing targets
        deactivateOldTargets();

        // 2. Add Fixed Targets (Requested by User)
        // 006620 (DongKoo Bio & Pharma), 314130 (Genome & Company)
        addTarget("006620", "동구바이오제약");
        addTarget("314130", "지놈앤컴퍼니");

        log.info("Phase 1: Screening Complete. Targets Updated.");
    }

    private void deactivateOldTargets() {
        List<TargetStock> activeStocks = targetStockRepository.findByIsActiveTrue();
        for (TargetStock stock : activeStocks) {
            stock.setActive(false);
        }
        targetStockRepository.saveAll(activeStocks);
        log.info("Deactivated {} old target stocks.", activeStocks.size());
    }

    // Renamed from createTarget for clarity and to match new logic
    private void addTarget(String symbol, String name) {
        // Default market/sector for simplified logic
        TargetStock target = TargetStock.builder()
                .symbol(symbol)
                .name(name)
                .market("KOSDAQ") // Assuming these are KOSDAQ for now or generic
                .sector("Bio/Pharma")
                .createdAt(LocalDateTime.now())
                .isActive(true)
                .build();
        targetStockRepository.save(target);
        log.info("Selected new target: {} ({})", name, symbol);
    }
}
