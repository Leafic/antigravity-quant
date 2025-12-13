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

    private final OrderService orderService;
    private final com.antigravity.trading.domain.strategy.TradingStrategy tradingStrategy;
    private final com.antigravity.trading.infrastructure.api.KisApiClient kisApiClient;
    private final org.springframework.context.ApplicationContext applicationContext; // For Lazy retrieval of
                                                                                     // WebsocketClient if needed

    // ... (rest of fields)
    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private com.antigravity.trading.infrastructure.websocket.KisWebSocketClient webSocketClient;

    // ... (init method)

    public void loadActiveTargets() {
        List<TargetStock> targets = targetStockRepository.findByIsActiveTrue();
        activeTargets.clear();
        for (TargetStock target : targets) {
            activeTargets.put(target.getSymbol(), target);
            if (webSocketClient != null) {
                webSocketClient.subscribe(target.getSymbol());
            }
        }
        log.info("Loaded {} active targets and subscribed.", activeTargets.size());
    }

    public void onTick(String payload) {
        // Payload Format: SYMBOL^TIME^PRICE^... (Delimiter might be ^ or | depending on
        // msg)
        // KIS Real Price: "0|H0STCNT0|001|SYMBOL^TIME^PRICE..."
        // Or if handled by Client splitter, just "SYMBOL^TIME^PRICE..."

        try {
            String[] parts = payload.split("\\^"); // Using Caret based on KIS docs for body
            if (parts.length < 3)
                return;

            String symbol = parts[0];
            String time = parts[1];
            java.math.BigDecimal currentPrice = new java.math.BigDecimal(parts[2]);
            // Volume? Index 13 usually accumulated volume?
            // Need robust parsing. For prototype, just Price.

            onPriceUpdate(symbol, currentPrice);
        } catch (Exception e) {
            log.error("Tick Parsing Error", e); // Verbose in prod
        }
    }

    public void onPriceUpdate(String symbol, java.math.BigDecimal currentPrice) {
        if (!killSwitchService.isSystemActive() || !activeTargets.containsKey(symbol)) {
            return;
        }

        // Fetch History for Context (e.g. Daily Candles)
        // Performance Note: Fetching API on every tick is too slow.
        // Should Cache history and update last candle in memory.
        // For Prototype: Fetch only every N seconds or Use Cache.
        // Let's Fetch Daily Chart (Limit 1 per minute per symbol? KIS rate limit 20
        // req/sec).
        // Strategy needs "Candles".

        // Optimize: Only analyze if (Time % 60 == 0) or similar?
        // Or fetch history async.

        // Logic:
        // 1. Get History (Sync for now, assume low traffic)
        // List<CandleDto> history = kisApiClient.getDailyChart(symbol)... -> mapped to
        // DTO
        // But KisApiClient returns Response. Need Controller's mapping reasoning or
        // direct usage.
        // Let's use KisApiClient result directly and map to DTO manually here.

        // Simplified for Speed: Just log tick for now. Strategy execution requires data
        // aggregation.
        log.debug("Tick: {} {}", symbol, currentPrice);

        // TODO: Full Strategy Execution
        // Example Logic (Skeleton):
        /*
         * List<CandleDto> candles = ...; // Fetch history
         * StrategySignal signal = tradingStrategy.analyze(symbol, candles);
         * 
         * if (signal.getType() == StrategySignal.SignalType.BUY) {
         * // 1. Send Signal to Group
         * notificationService.sendSignalNotification(symbol, "BUY",
         * signal.getReason());
         * 
         * // 2. Execute Order (Sends Trade Notification to Private)
         * orderService.buy(symbol, currentPrice, new BigDecimal("10"),
         * signal.getReason());
         * }
         */

        // For demonstration, we just log.
        log.debug("Processing Tick for {}: {}", symbol, currentPrice);
    }
}
