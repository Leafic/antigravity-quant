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
    public void runDailyScreening() {
        log.info("[Phase 1] Starting daily universe screening...");

        // 1. 기존 타겟 비활성화
        deactivateOldTargets();

        // 2. 새로운 타겟 선정 (Mock: Top 2 Fixed)
        selectNewTargets();

        log.info("[Phase 1] Universe screening completed.");
    }

    private void deactivateOldTargets() {
        List<TargetStock> activeStocks = targetStockRepository.findByIsActiveTrue();
        for (TargetStock stock : activeStocks) {
            stock.setActive(false);
        }
        targetStockRepository.saveAll(activeStocks);
        log.info("Deactivated {} old target stocks.", activeStocks.size());
    }

    private void selectNewTargets() {
        // 실제 로직: KIS API 등을 통해 거래대금 상위 종목 조회 및 필터링
        // Mock 로직: 삼성전자, SK하이닉스 고정 추가

        createTarget("005930", "삼성전자", "KOSPI", "Electrical/Electronic");
        createTarget("000660", "SK하이닉스", "KOSPI", "Electrical/Electronic");
    }

    private void createTarget(String symbol, String name, String market, String sector) {
        TargetStock target = TargetStock.builder()
                .symbol(symbol)
                .name(name)
                .market(market)
                .sector(sector)
                .createdAt(LocalDateTime.now())
                .isActive(true)
                .build();
        targetStockRepository.save(target);
        log.info("Selected new target: {} ({})", name, symbol);
    }
}
