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
public class PullbackStrategy implements TradingStrategy {

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Params extends StrategyParams {
        private int trendMaPeriod = 60;
        private int rsiPeriod = 14;
        private double rsiPullback = 40.0;
        private int atrPeriod = 14;
        private double atrTrailMult = 2.5;
    }

    @Override
    public String getId() {
        return "S2";
    }

    @Override
    public String getName() {
        return "S2 - RSI 눌림목 (Pullback)";
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
        if (event.getRsi() == null || event.getMa60() == null)
            return Signal.none();

        // 1. Trend Filter: Price > MA60 (or user configurable)
        // Assuming MarketEvent.ma60 is correct.
        if (event.getClose().compareTo(event.getMa60()) <= 0) {
            return Signal.none();
        }

        // Logic: Watch for RSI dip below params.rsiPullback
        Map<String, Object> extra = context.getExtraData();
        String OVERSOLD_KEY = "S2_Oversold";

        boolean wasOversold = extra != null && Boolean.TRUE.equals(extra.get(OVERSOLD_KEY));

        if (!context.isHasPosition()) {
            if (event.getRsi() <= params.rsiPullback) {
                // Record oversold state
                if (extra != null)
                    extra.put(OVERSOLD_KEY, true);
                return Signal.none(); // Wait for rebound
            }

            // Rebound Entry: Was Oversold AND Now RSI > Pullback (Cross Up)
            if (wasOversold && event.getRsi() > params.rsiPullback) {
                if (extra != null)
                    extra.put(OVERSOLD_KEY, false); // Reset

                // Init Trailing
                context.getExtraData().put("S2_highestHigh", event.getHigh().doubleValue());

                return Signal.builder()
                        .type(Signal.Type.BUY)
                        .strategyName(getName())
                        .reasonCode("PULLBACK_REBOUND")
                        .reasonMessageKo("추세(Above MA60) 눌림목 반등 (RSI " + params.rsiPullback + " 상향 돌파)")
                        .sizeFactor(1.0)
                        .confidence(0.7)
                        .build();
            }
        }
        // 2. Exit Logic (ATR Trailing)
        else {
            double currentPrice = event.getClose().doubleValue();
            double atr = event.getAtr() != null ? event.getAtr() : 0.0;

            double highestHigh = (double) context.getExtraData().getOrDefault("S2_highestHigh", currentPrice);
            if (currentPrice > highestHigh) {
                highestHigh = currentPrice;
                context.getExtraData().put("S2_highestHigh", highestHigh);
            }

            double stopPrice = highestHigh - (atr * params.atrTrailMult);

            if (currentPrice < stopPrice) {
                double pnlPercent = (currentPrice - context.getEntryPrice().doubleValue())
                        / context.getEntryPrice().doubleValue() * 100.0;
                return Signal.builder()
                        .type(Signal.Type.SELL)
                        .strategyName(getName())
                        .reasonCode("ATR_TRAIL_STOP")
                        .reasonMessageKo("ATR 트레일링 스탑 (눌림목 이후 추세 이탈, PnL: " + String.format("%.1f", pnlPercent) + "%)")
                        .sizeFactor(1.0)
                        .confidence(1.0)
                        .build();
            }
        }

        return Signal.none();
    }
}
