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

    // Strategy Specific State
    private int dailyEntryCount;
    private Map<String, Object> extraData;

    // V2: Historical Data Access (for dynamic lookback)
    // In backtest, this is the window up to current time.
    // In live, this is fetched from cache/db.
    private java.util.List<com.antigravity.trading.domain.dto.CandleDto> history;
}
