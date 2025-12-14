package com.antigravity.trading.engine.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Signal {
    public enum Type {
        BUY, SELL, HOLD, NONE
    }

    private final Type type;
    private final String symbol;
    private final String strategyName;
    private final String reasonCode; // e.g. BREAKOUT_MA20
    private final String reasonDetail; // Detailed explanation for DecisionLog
    private final String reasonMessageKo; // V2: User-friendly Korean reason
    private final double sizeFactor; // V2: 0.0 ~ 1.0
    private final double confidence; // 0.0 ~ 1.0 (Optional)

    public static Signal none() {
        return Signal.builder().type(Type.NONE).reasonMessageKo("").sizeFactor(0.0).build();
    }
}
