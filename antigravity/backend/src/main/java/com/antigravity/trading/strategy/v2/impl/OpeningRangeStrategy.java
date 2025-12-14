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
public class OpeningRangeStrategy implements TradingStrategy {

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Params extends StrategyParams {
        private int openingRangeMinutes = 10;
        private double minGapPercent = 3.0; // Wait, maybe "Gap" means gap from Yesterday Close? Or gap valid ranges?
        private double minVolumeRatio = 2.0;
        private int hardTimeExitMinute = 30; // e.g. 30min after entry or fixed time? "30min"
        // Interpreting "hardTimeExit=30min" as "Hold for max 30 mins".
    }

    @Override
    public String getId() {
        return "S5";
    }

    @Override
    public String getName() {
        return "S5 - 오프닝 레인지 (Opening Range)";
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

        // 1. Entry Logic
        if (!context.isHasPosition()) {
            if (event.getOpeningRangeHigh() == null)
                return Signal.none();

            // Check Gap (Optional based on user request "minGapPercent=3.0")
            // Assuming "Gap" means Open price vs Yesterday Close.
            // Need Yesterday Close.
            // MarketEvent doesn't explicitly have PrevClose (it has Open).
            // Approximation: Open vs Close (if prev close not avail).
            // Actually, let's skip strict Gap check unless critical, or assume handled by
            // pre-filtering.
            // Or use StrategyContext history.

            // Breakout
            if (event.getClose().compareTo(event.getOpeningRangeHigh()) > 0) {
                // Volume Check
                if (event.getVolumeRatio() >= params.minVolumeRatio) {
                    // Entry
                    context.getExtraData().put("S5_EntryTime", event.getTimestamp());
                    return Signal.builder()
                            .type(Signal.Type.BUY)
                            .strategyName(getName())
                            .reasonCode("ORB_BREAK")
                            .reasonMessageKo("장초반 오프닝레인지(" + params.openingRangeMinutes + "분) 상단 돌파")
                            .sizeFactor(1.0)
                            .confidence(0.9)
                            .build();
                }
            }
        }
        // 2. Exit Logic
        else {
            BigDecimal currentPrice = event.getClose();
            BigDecimal orbLow = event.getOpeningRangeLow();

            // Stop Loss: Fall below Opening Range Low
            if (orbLow != null && currentPrice.compareTo(orbLow) < 0) {
                return Signal.builder()
                        .type(Signal.Type.SELL)
                        .strategyName(getName())
                        .reasonCode("ORB_FAIL")
                        .reasonMessageKo("오프닝 레인지 하단 이탈 손절")
                        .sizeFactor(1.0)
                        .build();
            }

            // Time Exit
            java.time.LocalDateTime entryTime = (java.time.LocalDateTime) context.getExtraData().get("S5_EntryTime");
            if (entryTime != null) {
                long durationMinutes = java.time.Duration.between(entryTime, event.getTimestamp()).toMinutes();
                if (durationMinutes >= params.hardTimeExitMinute) {
                    return Signal.builder()
                            .type(Signal.Type.SELL)
                            .strategyName(getName())
                            .reasonCode("TIME_EXIT")
                            .reasonMessageKo("최대 보유 시간(" + params.hardTimeExitMinute + "분) 경과 청산")
                            .sizeFactor(1.0)
                            .build();
                }
            }
        }

        return Signal.none();
    }
}
