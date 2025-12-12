package com.antigravity.trading.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class RiskManagementService {

    private final AtomicReference<BigDecimal> dailyLossLimitPercent = new AtomicReference<>(new BigDecimal("3.0")); // Default
                                                                                                                    // 3%
    private final AtomicReference<BigDecimal> currentDailyLoss = new AtomicReference<>(BigDecimal.ZERO);

    public void setDailyLossLimit(BigDecimal limitPercent) {
        if (limitPercent.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }
        this.dailyLossLimitPercent.set(limitPercent);
        log.info("Daily Loss Limit set to {}%", limitPercent);
    }

    public BigDecimal getDailyLossLimit() {
        return dailyLossLimitPercent.get();
    }

    public boolean isTradingAllowed() {
        // TODO: Logic to check current PnL vs Limit
        // For now, always allowed unless overridden
        return true;
    }
}
