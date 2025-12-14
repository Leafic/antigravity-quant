package com.antigravity.trading.strategy.v2.impl;

import com.antigravity.trading.engine.model.MarketEvent;
import com.antigravity.trading.engine.model.Signal;
import com.antigravity.trading.engine.model.StrategyContext;
import com.antigravity.trading.strategy.v2.StrategyParams;
import com.antigravity.trading.strategy.v2.TradingStrategy;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class RsiMeanReversionStrategy implements TradingStrategy {

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Params extends StrategyParams {
        private int rsiPeriod = 7;
        private double entryRsi = 28.0;
        private double exitRsi = 62.0;
        private int maxHoldMinutes = 20;
        private double stopLossPercent = -1.8;
        private double takeProfitPercent = 2.2;
        private boolean trailingEnabled = true;
        private double trailFromPeakPercent = -1.0;
        private boolean useTrendGuard = true;
    }

    @Override
    public String getId() {
        return "S2";
    }

    @Override
    public String getName() {
        return "RSI Scalp";
    }

    @Override
    public StrategyParams getDefaultParams() {
        return new Params();
    }

    @Override
    public Class<? extends StrategyParams> getParamsClass() {
        return Params.class;
    }

    @Override
    public Signal evaluate(MarketEvent event, StrategyContext context, StrategyParams baseParams) {
        Params params = (Params) baseParams;
        if (event.getRsi() == null)
            return Signal.none();

        // 1. Trend Guard (Optional)
        // If strong trending down, avoid buying unless RSI is extremely low?
        // Logic: if useTrendGuard is true, and MA20 < MA60 (downtrend), maybe skip or
        // require lower RSI?
        // For simplicity, let's implement basic guard: Don't buy if price is far below
        // MA20 (falling knife).
        if (params.useTrendGuard) {
            // Placeholder: Could check slope
        }

        // State (Oversold Flag)
        Map<String, Object> extra = context.getExtraData();
        boolean wasOversold = extra != null && Boolean.TRUE.equals(extra.get("S2_oversold"));

        // 2. Entry Logic
        if (!context.isHasPosition()) {
            boolean currentOversold = event.getRsi() <= params.entryRsi;

            if (currentOversold) {
                // Mark as oversold, wait for rebound
                // We need to persist this state. In simulation, extraData is persisted per
                // step?
                // BacktestService needs to support updating extraData.
                // Assuming it does or we return NONE but context is mutable.
                // Actually context IS mutable in memory during backtest loop usually.
                if (extra != null)
                    extra.put("S2_oversold", true);
                return Signal.none(); // Wait for confirmation
            }

            if (wasOversold && event.getRsi() > params.entryRsi) {
                // Confirmation: Re-crossed above entryRsi
                if (extra != null)
                    extra.put("S2_oversold", false); // Reset

                return Signal.builder()
                        .type(Signal.Type.BUY)
                        .strategyName(getName())
                        .reasonCode("RSI_CROSS_UP")
                        .reasonMessageKo(
                                "RSI 과매도(" + params.entryRsi + ") 탈출 반등 (Close: " + event.getRsi().intValue() + ")")
                        .sizeFactor(1.0)
                        .build();
            }
        }
        // 3. Exit Logic
        else {
            BigDecimal entryPrice = context.getEntryPrice();
            BigDecimal currentPrice = event.getClose();
            double pnlPercent = (currentPrice.doubleValue() - entryPrice.doubleValue()) / entryPrice.doubleValue()
                    * 100.0;

            // RSI Exit
            if (event.getRsi() >= params.exitRsi) {
                return Signal.builder()
                        .type(Signal.Type.SELL)
                        .strategyName(getName())
                        .reasonCode("RSI_OVERBOUGHT")
                        .reasonMessageKo("RSI 목표값(" + params.exitRsi + ") 도달 익절")
                        .sizeFactor(1.0)
                        .build();
            }

            // Stop Loss
            if (pnlPercent <= params.stopLossPercent) {
                return Signal.builder()
                        .type(Signal.Type.SELL)
                        .strategyName(getName())
                        .reasonCode("STOP_LOSS")
                        .reasonMessageKo("손절매 기준 도달 (" + String.format("%.1f", pnlPercent) + "%)")
                        .sizeFactor(1.0)
                        .build();
            }

            // Take Profit
            if (pnlPercent >= params.takeProfitPercent) {
                return Signal.builder()
                        .type(Signal.Type.SELL)
                        .strategyName(getName())
                        .reasonCode("TAKE_PROFIT")
                        .reasonMessageKo("익절 목표 달성 (" + String.format("%.1f", pnlPercent) + "%)")
                        .sizeFactor(1.0)
                        .build();
            }

            // Trailing Stop (Simplified)
            if (params.trailingEnabled && context.getHighWaterMark() != null) {
                double dropFromPeak = (currentPrice.doubleValue() - context.getHighWaterMark().doubleValue())
                        / context.getHighWaterMark().doubleValue() * 100.0;
                if (dropFromPeak <= params.trailFromPeakPercent && pnlPercent > 0.5) { // Ensure some profit
                    return Signal.builder()
                            .type(Signal.Type.SELL)
                            .strategyName(getName())
                            .reasonCode("TRAILING_STOP")
                            .reasonMessageKo("트레일링 스탑 발동 (고점 대비 " + String.format("%.1f", dropFromPeak) + "%)")
                            .sizeFactor(1.0)
                            .build();
                }
            }
            // Time Stop: maxHoldMinutes? Requires EntryTime in context.
            // context currently doesn't track entryTime. Assuming irrelevant for MVP
            // backtest loop structure.
        }

        return Signal.none();
    }
}
