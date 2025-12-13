package com.antigravity.trading.engine.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@Builder
public class StrategyContext {
    private final String symbol;

    // Position Info
    private boolean hasPosition;
    private BigDecimal entryPrice;
    private long quantity;
    private BigDecimal highWaterMark; // For Trailing Stop
    private BigDecimal unrealizedPnlPct;

    // Account Info (for sizing)
    private BigDecimal availableCash;

    // Strategy Specific State (e.g. today's entry count)
    private int dailyEntryCount;
    private Map<String, Object> extraData;
}
