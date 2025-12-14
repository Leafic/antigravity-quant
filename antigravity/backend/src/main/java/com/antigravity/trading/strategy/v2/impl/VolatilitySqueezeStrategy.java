package com.antigravity.trading.strategy.v2.impl;

import com.antigravity.trading.domain.dto.CandleDto;
import com.antigravity.trading.engine.model.MarketEvent;
import com.antigravity.trading.engine.model.Signal;
import com.antigravity.trading.engine.model.StrategyContext;
import com.antigravity.trading.strategy.v2.StrategyParams;
import com.antigravity.trading.strategy.v2.TradingStrategy;
import com.antigravity.trading.util.TechnicalIndicators;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class VolatilitySqueezeStrategy implements TradingStrategy {

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Params extends StrategyParams {
        private int percentileWindow = 100;
        private double atrPercentileThreshold = 20.0;
        private double breakoutBufferPercent = 0.2;
        private double volumeMultiplier = 2.0;
        private int atrPeriod = 14;
        private double atrTrailMult = 2.5;
    }

    @Override
    public String getId() {
        return "S3";
    }

    @Override
    public String getName() {
        return "S3 - 변동성 돌파 (Vol Squeeze)";
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
            if (event.getAtr() == null)
                return Signal.none();

            // Check ATR Percentile
            // Need History
            List<CandleDto> history = context.getHistory();
            if (history == null || history.size() < params.percentileWindow)
                return Signal.none();

            // Re-calc ATR history needed? Or assume pre-calculated and cached somewhere?
            // Calculating ATR for 100 candles every step is expensive inside loop?
            // Better if BacktestService passes pre-calculated indicator series or S3
            // calculates once.
            // For now, calculate on the fly (performance hit but correct logic).
            // Actually, simplified approach: Compare current ATR to Min/Max of last N ATRs?
            // "Percentile < 20%" logic requires distribution.
            // Let's implement simpler: ATR < (Min(ATR 100) + (Max-Min)*0.2) ?

            // Optimization: MarketEvent could provide "atrPercentile" if indicators engine
            // supports it.
            // But doing it here:
            // Fetch last N candles
            List<CandleDto> recent = history.subList(Math.max(0, history.size() - params.percentileWindow),
                    history.size());
            List<Double> atrs = TechnicalIndicators.calculateAtr(recent, 14); // 14-period ATR

            if (atrs.isEmpty())
                return Signal.none();
            double currentAtr = atrs.get(atrs.size() - 1);

            // Rank
            int rank = 0;
            int count = 0;
            for (Double a : atrs) {
                if (a != null) {
                    if (a < currentAtr)
                        rank++;
                    count++;
                }
            }
            double percentile = (double) rank / count * 100.0;
            boolean isSqueeze = percentile <= params.atrPercentileThreshold;

            // Breakout Condition
            // Need Donchian High effectively? Or just recent High?
            // Using MarketEvent.donchianHigh (20d default)
            BigDecimal trigger = event.getDonchianHigh();
            if (trigger == null)
                trigger = event.getHigh(); // Fallback

            if (isSqueeze) {
                // If squeeze is ON, we watch for breakout.
                // Store "Squeeze Active" in context?
                context.getExtraData().put("S3_Squeeze", true);
            }

            // If Squeeze was active recently (or now) AND Breakout
            boolean wasSqueeze = Boolean.TRUE.equals(context.getExtraData().get("S3_Squeeze"));

            if (wasSqueeze && event.getClose().compareTo(trigger) > 0) {
                if (event.getVolumeRatio() >= params.volumeMultiplier) {
                    context.getExtraData().put("S3_Squeeze", false); // Reset
                    context.getExtraData().put("S3_highestHigh", event.getHigh().doubleValue()); // ITrail

                    return Signal.builder()
                            .type(Signal.Type.BUY)
                            .strategyName(getName())
                            .reasonCode("SQUEEZE_BREAK")
                            .reasonMessageKo("변동성 수축(ATR상위 " + String.format("%.0f", percentile) + "%) 후 거래량 실린 돌파")
                            .sizeFactor(1.0)
                            .confidence(0.9)
                            .build();
                }
            }
        }
        // 2. Exit Logic (ATR Trail)
        else {
            double currentPrice = event.getClose().doubleValue();
            double atr = event.getAtr() != null ? event.getAtr() : 0.0;

            double highestHigh = (double) context.getExtraData().getOrDefault("S3_highestHigh", currentPrice);
            if (currentPrice > highestHigh) {
                highestHigh = currentPrice;
                context.getExtraData().put("S3_highestHigh", highestHigh);
            }

            double stopPrice = highestHigh - (atr * params.atrTrailMult);
            if (currentPrice < stopPrice) {
                return Signal.builder()
                        .type(Signal.Type.SELL)
                        .strategyName(getName())
                        .reasonCode("ATR_TRAIL_STOP")
                        .reasonMessageKo("ATR 트레일링 스탑 (변동성 확장 후 이탈)")
                        .sizeFactor(1.0)
                        .confidence(1.0)
                        .build();
            }
        }

        return Signal.none();
    }
}
