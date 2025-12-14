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
public class DonchianStrategy implements TradingStrategy {

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Params extends StrategyParams {
        private int donchianPeriod = 20;
        private double volumeMultiplier = 1.8;
        private int atrPeriod = 14;
        private double atrStopMult = 2.0;
        private double atrTrailMult = 2.5;
    }

    @Override
    public String getId() {
        return "S1";
    }

    @Override
    public String getName() {
        return "S1 - 돈치안 돌파 (Donchian)";
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
            if (event.getDonchianHigh() == null)
                return Signal.none();

            // Breakout: Current Close > Previous N-day High (Donchian High)
            // Note: Data layer should ensure 'donchianHigh' represents previous period's
            // high.
            // If data includes today in donchian calc, checking > donchianHigh is
            // impossible (it equals).
            // Assuming BacktestService passes Previous Donchian High.

            if (event.getClose().compareTo(event.getDonchianHigh()) > 0) {
                // Filter: Volume
                if (event.getVolumeRatio() >= params.volumeMultiplier) {
                    double atr = event.getAtr() != null ? event.getAtr() : 0.0;
                    // Initial Stop
                    double stopPrice = event.getClose().doubleValue() - (atr * params.atrStopMult);

                    context.getExtraData().put("S1_highestHigh", event.getHigh().doubleValue()); // Init Trailing High

                    return Signal.builder()
                            .type(Signal.Type.BUY)
                            .strategyName(getName())
                            .reasonCode("DONCHIAN_BREAK")
                            .reasonMessageKo("신고가 돌파(Donchian " + params.donchianPeriod + "일) + 거래량 "
                                    + String.format("%.1f", event.getVolumeRatio()) + "배")
                            .sizeFactor(1.0)
                            .confidence(0.8)
                            .build();
                }
            }
        }
        // 2. Exit Logic (Trailing Stop)
        else {
            double currentPrice = event.getClose().doubleValue();
            double atr = event.getAtr() != null ? event.getAtr() : 0.0;

            // Update Highest High since Entry
            double highestHigh = (double) context.getExtraData().getOrDefault("S1_highestHigh", currentPrice);
            if (currentPrice > highestHigh) {
                highestHigh = currentPrice;
                context.getExtraData().put("S1_highestHigh", highestHigh);
            }

            // Chandelier Exit: Highest High - ATR * Mult
            double stopPrice = highestHigh - (atr * params.atrTrailMult);

            if (currentPrice < stopPrice) {
                double pnlPercent = (currentPrice - context.getEntryPrice().doubleValue())
                        / context.getEntryPrice().doubleValue() * 100.0;
                return Signal.builder()
                        .type(Signal.Type.SELL)
                        .strategyName(getName())
                        .reasonCode("ATR_TRAIL_STOP")
                        .reasonMessageKo("ATR 트레일링 스탑 (고점 대비 이탈, PnL: " + String.format("%.1f", pnlPercent) + "%)")
                        .sizeFactor(1.0)
                        .confidence(1.0)
                        .build();
            }
        }

        return Signal.none();
    }
}
