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

@Component
public class TrendMomentumStrategy implements TradingStrategy {

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Params extends StrategyParams {
        private int lookbackHighBars = 20;
        private double breakoutBufferPercent = 0.2;
        private int avgVolumeWindow = 20;
        private double volumeMultiplier = 1.5;
        private double tp1Percent = 3.0;
        private double tp1SizeFraction = 0.5;
        private double stopLossPercent = -2.5;
        private boolean useSma60Filter = true;
    }

    @Override
    public String getId() {
        return "S1";
    }

    @Override
    public String getName() {
        return "Trend Breakout";
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

        // 1. Trend Filter
        // price > SMA20
        if (event.getMa20() == null || event.getClose().compareTo(event.getMa20()) <= 0) {
            return Signal.none();
        }

        // SMA20 > SMA60 (if enabled)
        if (params.useSma60Filter && (event.getMa60() == null || event.getMa20().compareTo(event.getMa60()) <= 0)) {
            return Signal.none();
        }

        // 2. Entry Logic (Long)
        if (!context.isHasPosition()) {
            boolean breakout = false;
            // Check Breakout of 20-day High
            // event.breakoutPrice is pre-calculated "20-day High" from BacktestService
            // (need to verify mapping)
            // Logic: Close > Previous High (breakoutPrice) * (1 + buffer)
            // Wait, breakoutPrice in MarketEvent usually refers to the resistance level.
            if (event.getBreakoutPrice() != null) {
                BigDecimal threshold = event.getBreakoutPrice()
                        .multiply(BigDecimal.valueOf(1.0 + params.breakoutBufferPercent / 100.0));
                if (event.getClose().compareTo(threshold) > 0) {
                    breakout = true;
                }
            }

            // Check Volume
            boolean volCond = false;
            if (event.getVolumeRatio() >= params.volumeMultiplier) {
                volCond = true;
            }

            if (breakout && volCond) {
                return Signal.builder()
                        .type(Signal.Type.BUY)
                        .strategyName(getName())
                        .reasonCode("BREAKOUT_VOL")
                        .reasonMessageKo("전일 고가 돌파 및 거래량 급증 (" + String.format("%.1f", event.getVolumeRatio()) + "배)")
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

            // Take Profit 1 (Partial) - Not fully supported by single signal yet?
            // "tp1SizeFraction (50%)".
            // If Signal supports sizeFactor, we can do 0.5.
            // But state management needs to know if we already TP'd.
            // StrategyContext.extraData can track "tp1Executed".
            boolean tp1Executed = context.getExtraData() != null
                    && Boolean.TRUE.equals(context.getExtraData().get("tp1Executed"));

            if (!tp1Executed && pnlPercent >= params.tp1Percent) {
                return Signal.builder()
                        .type(Signal.Type.SELL)
                        .strategyName(getName())
                        .reasonCode("TAKE_PROFIT_1")
                        .reasonMessageKo("1차 목표가 도달 (" + String.format("%.1f", pnlPercent) + "%) - 부분 익절")
                        .sizeFactor(params.tp1SizeFraction)
                        .build();
            }

            // Trend Exit: Close < SMA20
            if (event.getMa20() != null && currentPrice.compareTo(event.getMa20()) < 0) {
                return Signal.builder()
                        .type(Signal.Type.SELL)
                        .strategyName(getName())
                        .reasonCode("TREND_BROKEN")
                        .reasonMessageKo("추세 이탈 (종가 < 20이평)")
                        .sizeFactor(1.0) // Sell remaining
                        .build();
            }
        }

        return Signal.none();
    }
}
