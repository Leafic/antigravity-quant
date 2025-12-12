package com.antigravity.trading.domain.strategy;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class StrategySignal {
    private String symbol;
    private SignalType type; // BUY, SELL, HOLD
    private BigDecimal price;
    private String reason;

    public enum SignalType {
        BUY, SELL, HOLD
    }
}
