package com.antigravity.trading.service;

import com.antigravity.trading.domain.entity.TargetStock;
import com.antigravity.trading.repository.TargetStockRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Phase 3: Real-Time Trader
 * 장중 실시간 시세(WebSocket)를 수신하고 매매 로직을 수행합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealTimeTrader {

    private final TargetStockRepository targetStockRepository;
    private final KillSwitchService killSwitchService;
    private final NotificationService notificationService;

    // 메모리에 로드된 활성 타겟 목록 (빠른 조회를 위해 캐싱)
    private final ConcurrentHashMap<String, TargetStock> activeTargets = new ConcurrentHashMap<>();

    /**
     * 애플리케이션 시작 시(혹은 장 시작 시) 활성 타겟 로드
     */
    @PostConstruct
    public void init() {
        log.info("[Phase 3] Initializing RealTimeTrader...");
        loadActiveTargets();
    }

    public void loadActiveTargets() {
        List<TargetStock> targets = targetStockRepository.findByIsActiveTrue();
        activeTargets.clear();
        for (TargetStock target : targets) {
            activeTargets.put(target.getSymbol(), target);
            // TODO: Subscribe to WebSocket for this symbol
        }
        log.info("Loaded {} active targets into memory.", activeTargets.size());
    }

    /**
     * 실시간 가격 수신 시 호출되는 메서드 (WebSocket 핸들러에서 호출 예정)
     */
    public void onPriceUpdate(String symbol, java.math.BigDecimal currentPrice) {
        if (!killSwitchService.isSystemActive()) {
            log.warn("Price update blocked by Kill Switch for {}", symbol);
            return;
        }

        if (!activeTargets.containsKey(symbol)) {
            return; // 관심 종목 아니면 무시
        }

        // Trading Logic Placeholder
        // 1. Check Kill Switch (Done above)
        // 2. Compare Price with MA (from DB or Cache)
        // 3. Execute Trade
        log.debug("Price update for {}: {}", symbol, currentPrice);

        // Mock Trade Execution for Demo
        // notificationService.sendTradeAlert(...);
    }
}
