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

    // Configurable Parameters (Defaults are STRICT)
    @lombok.Data
    @lombok.Builder
    private static class Config {
        private LocalTime entryStartTime;
        private LocalTime entryEndTime;
        private BigDecimal volumeRatioThreshold;
        private BigDecimal breakoutBuffer;
        private boolean strictMaSlope;

        public static Config strict() {
            return Config.builder()
                    .entryStartTime(LocalTime.of(9, 10))
                    .entryEndTime(LocalTime.of(14, 50))
                    .volumeRatioThreshold(new BigDecimal("2.0")) // 200%
                    .breakoutBuffer(new BigDecimal("1.002")) // 0.2%
                    .strictMaSlope(true)
                    .build();
        }

        public static Config loose() {
            return Config.builder()
                    .entryStartTime(LocalTime.of(9, 5))
                    .entryEndTime(LocalTime.of(15, 0))
                    .volumeRatioThreshold(new BigDecimal("1.2")) // 120%
                    .breakoutBuffer(new BigDecimal("1.0005")) // 0.05%
                    .strictMaSlope(false)
                    .build();
        }
    }

    private static final LocalTime EOD_EXIT_TIME = LocalTime.of(15, 15);
    private static final BigDecimal SPREAD_THRESHOLD = new BigDecimal("0.005"); // 0.5%
    private static final BigDecimal STOP_LOSS_PCT = new BigDecimal("0.012"); // 1.2%
    private static final BigDecimal TAKE_PROFIT_PCT = new BigDecimal("0.020"); // 2.0%
    private static final BigDecimal TRAILING_STOP_PCT = new BigDecimal("0.008"); // 0.8%

    @Override
    public String getId() {
        return "S1";
    }

    @Override
    public String getName() {
        return "1) 돌파+거래량 (TrendMomentum)";
    }

    @Override
    public String getDescription() {
        return "전일 고가 돌파 및 거래량 증가 시 진입하는 단타 전략";
    }

    @Override
    public String getDefaultParamsJson() {
        return "{\"mode\":\"STRICT\", \"desc\":\"Strict Mode\"}";
    }

    @Override
    public Signal analyze(MarketEvent event, StrategyContext context) {
        LocalTime now = event.getTimestamp().toLocalTime();

        // 0. Determine Mode
        String mode = (String) context.getExtraData().getOrDefault("mode", "STRICT");
        Config config = "LOOSE".equalsIgnoreCase(mode) ? Config.loose() : Config.strict();

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
        if (now.isBefore(config.getEntryStartTime()) || now.isAfter(config.getEntryEndTime())) {
            return rejection("REJECT_TIME");
        }

        // Spread Filter (Strict only, or apply generally. User asked to disable in
        // Backtest if needed, implemented as Strict check for now)
        if (event.getSpreadPct() != null && event.getSpreadPct().compareTo(SPREAD_THRESHOLD) > 0) {
            return rejection("REJECT_SPREAD");
        }

        // Main Trend Logic (Price > MA20)
        BigDecimal currentPrice = event.getCurrentPrice();
        if (event.getMa20() != null && currentPrice.compareTo(event.getMa20()) <= 0) {
            return rejection("REJECT_MA20_TREND");
        }

        if (config.isStrictMaSlope() && !event.isMa20Rising()) {
            return rejection("REJECT_MA20_SLOPE");
        }

        // Breakout Logic
        BigDecimal breakoutLevel = event.getBreakoutPrice();
        if (breakoutLevel != null) {
            // Price > Breakout * Buffer
            BigDecimal target = breakoutLevel.multiply(config.getBreakoutBuffer());

            if (currentPrice.compareTo(target) >= 0) {
                // Volume Surge Check
                if (event.getVolumeRatio() >= config.getVolumeRatioThreshold().doubleValue()) {
                    return Signal.builder()
                            .type(Signal.Type.BUY)
                            .strategyName(getName())
                            .reasonCode("BREAKOUT_VOL")
                            .reasonDetail("Breakout > " + breakoutLevel + " with Vol Rate " + event.getVolumeRatio())
                            .build();
                } else {
                    return rejection("REJECT_VOLUME");
                }
            } else {
                return rejection("REJECT_PRICE_BELOW_TARGET");
            }
        }

        return rejection("REJECT_NO_SETUP");
    }

    private Signal rejection(String code) {
        return Signal.builder()
                .type(Signal.Type.NONE)
                .reasonCode(code)
                .build();
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

        return Signal.builder().type(Signal.Type.HOLD).build();
    }
}
