package com.antigravity.trading.engine.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder(toBuilder = true)
@ToString
public class MarketEvent {
    private final String symbol;
    private final LocalDateTime timestamp;
    private final BigDecimal currentPrice;
    private final BigDecimal open;
    private final BigDecimal high;
    private final BigDecimal low;
    private final BigDecimal close;
    private final long volume;

    // Derived/Snapshot Data
    private final BigDecimal ma20;
    private final BigDecimal ma60;
    private final BigDecimal avgVol20;
    private final BigDecimal spreadPct; // (Ask - Bid) / Mid

    // Strategy Specific Indicators
    private final BigDecimal breakoutPrice; // Target Level
    private final boolean isMa20Rising; // Slope check
    private final double volumeRatio; // Current Vol / Avg Vol

    public boolean isValid() {
        return currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0;
    }
}
