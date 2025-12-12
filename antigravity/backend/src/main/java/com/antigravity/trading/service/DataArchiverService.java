package com.antigravity.trading.service;

import com.antigravity.trading.domain.entity.CandleHistory;
import com.antigravity.trading.domain.entity.TargetStock;
import com.antigravity.trading.infrastructure.api.KisApiClient;
import com.antigravity.trading.repository.CandleHistoryRepository;
import com.antigravity.trading.repository.TargetStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Phase 2: Data Archiver Service
 * 스크리닝된 종목들의 과거 데이터를 수집하고 보조지표(이평선 등)를 사전 계산하여 저장합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataArchiverService {

    private final TargetStockRepository targetStockRepository;
    private final CandleHistoryRepository candleHistoryRepository;
    private final KisApiClient kisApiClient;

    /**
     * 데이터 아카이빙 배치 작업
     * Universe Screening 직후 실행 (예: 08:35)
     */
    @Scheduled(cron = "0 35 8 * * *")
    @Transactional
    public void runDailyArchiving() {
        log.info("[Phase 2] Starting daily data archiving...");

        // 1. 활성 타겟 조회
        List<TargetStock> targets = targetStockRepository.findByIsActiveTrue();
        if (targets.isEmpty()) {
            log.warn("No active targets found. Skipping archiving.");
            return;
        }

        // 2. 각 타겟별 데이터 수집 및 계산
        for (TargetStock target : targets) {
            processTarget(target);
        }

        log.info("[Phase 2] Data archiving completed for {} targets.", targets.size());
    }

    private void processTarget(TargetStock target) {
        log.debug("Processing target: {}", target.getName());

        // TODO: 실제 API 호출 및 데이터 저장 로직 구현 (Mock)
        // kisApiClient.getMinuteCandles(...)

        // Mock: 현재 시점에 가상의 캔들 하나 저장
        CandleHistory mockCandle = CandleHistory.builder()
                .symbol(target.getSymbol())
                .time(LocalDateTime.now().minusMinutes(1))
                .open(new BigDecimal("70000"))
                .high(new BigDecimal("70500"))
                .low(new BigDecimal("69900"))
                .close(new BigDecimal("70200"))
                .volume(10000L)
                .ma20(new BigDecimal("69800")) // Pre-calculated
                .ma60(new BigDecimal("69500")) // Pre-calculated
                .build();

        candleHistoryRepository.save(mockCandle);
    }
}
