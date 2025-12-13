package com.antigravity.trading.engine.strategy;

import com.antigravity.trading.engine.StrategyEngine;
import com.antigravity.trading.engine.model.MarketEvent;
import com.antigravity.trading.engine.model.Signal;
import com.antigravity.trading.engine.model.StrategyContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;

@Slf4j
@Component
public class TrendMomentumScalpV1 implements StrategyEngine {

    // Configurable Parameters
    private static final LocalTime ENTRY_START_TIME = LocalTime.of(9, 10);
    private static final LocalTime ENTRY_END_TIME = LocalTime.of(14, 50);
    private static final LocalTime EOD_EXIT_TIME = LocalTime.of(15, 15);

    private static final BigDecimal SPREAD_THRESHOLD = new BigDecimal("0.005"); // 0.5%
    private static final BigDecimal STOP_LOSS_PCT = new BigDecimal("0.012"); // 1.2%
    private static final BigDecimal TAKE_PROFIT_PCT = new BigDecimal("0.020"); // 2.0%
    private static final BigDecimal TRAILING_STOP_PCT = new BigDecimal("0.008"); // 0.8%

    @Override
    public String getName() {
        return "TrendMomentumScalpV1";
    }

    @Override
    public Signal analyze(MarketEvent event, StrategyContext context) {
        LocalTime now = event.getTimestamp().toLocalTime();

        // 1. EOD Force Exit
        if (now.isAfter(EOD_EXIT_TIME) && context.isHasPosition()) {
            return Signal.builder()
                    .type(Signal.Type.SELL)
                    .strategyName(getName())
                    .reasonCode("EOD_FLATTEN")
                    .reasonDetail("End of Day Force Exit")
                    .build();
        }

        // 2. Manage Existing Position (Exit Logic)
        if (context.isHasPosition()) {
            return checkExitRules(event, context);
        }

        // 3. Entry Logic
        // Time Filter
        if (now.isBefore(ENTRY_START_TIME) || now.isAfter(ENTRY_END_TIME)) {
            return Signal.none();
        }

        // Spread Filter
        if (event.getSpreadPct() != null && event.getSpreadPct().compareTo(SPREAD_THRESHOLD) > 0) {
            return Signal.none(); // Spread too high
        }

        // Main Trend Logic (Price > MA20)
        BigDecimal currentPrice = event.getCurrentPrice();
        if (event.getMa20() != null && currentPrice.compareTo(event.getMa20()) <= 0) {
            return Signal.none(); // Below MA20
        }

        if (!event.isMa20Rising()) {
            return Signal.none(); // MA20 not rising
        }

        // Breakout Logic
        BigDecimal breakoutLevel = event.getBreakoutPrice();
        if (breakoutLevel != null) {
            // 1) Price > Breakout * 1.002
            BigDecimal target = breakoutLevel.multiply(new BigDecimal("1.002"));

            if (currentPrice.compareTo(target) >= 0) {
                // Volume Surge Check
                if (event.getVolumeRatio() >= 0.5) {
                    return Signal.builder()
                            .type(Signal.Type.BUY)
                            .strategyName(getName())
                            .reasonCode("BREAKOUT_VOL")
                            .reasonDetail("Breakout > " + breakoutLevel + " with Vol Ratio " + event.getVolumeRatio())
                            .build();
                }
            }
        }

        return Signal.none();
    }

    private Signal checkExitRules(MarketEvent event, StrategyContext context) {
        BigDecimal currentPrice = event.getCurrentPrice();
        BigDecimal entryPrice = context.getEntryPrice();

        // PnL Calculation
        BigDecimal pnlPct = currentPrice.subtract(entryPrice)
                .divide(entryPrice, 4, RoundingMode.HALF_UP);

        // Stop Loss
        if (pnlPct.compareTo(STOP_LOSS_PCT.negate()) <= 0) {
            return Signal.builder()
                    .type(Signal.Type.SELL)
                    .strategyName(getName())
                    .reasonCode("STOP_LOSS")
                    .reasonDetail("Loss exceeds 1.2%")
                    .build();
        }

        // Take Profit
        if (pnlPct.compareTo(TAKE_PROFIT_PCT) >= 0) {
            return Signal.builder()
                    .type(Signal.Type.SELL)
                    .strategyName(getName())
                    .reasonCode("TAKE_PROFIT")
                    .reasonDetail("Profit target reached 2.0%")
                    .build();
        }

        // Trailing Stop (High Water Mark logic usually needed in Context)
        if (context.getHighWaterMark() != null) {
            BigDecimal drawdown = context.getHighWaterMark().subtract(currentPrice)
                    .divide(context.getHighWaterMark(), 4, RoundingMode.HALF_UP);
            if (drawdown.compareTo(TRAILING_STOP_PCT) >= 0) {
                return Signal.builder()
                        .type(Signal.Type.SELL)
                        .strategyName(getName())
                        .reasonCode("TRAIL_STOP")
                        .reasonDetail("Price dropped 0.8% from peak")
                        .build();
            }
        }

        return Signal.none();
    }
}
