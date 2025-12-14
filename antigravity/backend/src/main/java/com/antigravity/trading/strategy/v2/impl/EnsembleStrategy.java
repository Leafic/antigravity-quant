package com.antigravity.trading.strategy.v2.impl;

import com.antigravity.trading.engine.model.MarketEvent;
import com.antigravity.trading.engine.model.Signal;
import com.antigravity.trading.engine.model.StrategyContext;
import com.antigravity.trading.strategy.v2.StrategyParams;
import com.antigravity.trading.strategy.v2.TradingStrategy;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnsembleStrategy implements TradingStrategy {

    private final DonchianStrategy s1;
    private final PullbackStrategy s2;
    private final VolatilitySqueezeStrategy s3;

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Params extends StrategyParams {
        private double w1 = 0.4; // Trend Weight
        private double w2 = 0.3; // RSI Weight
        private double w3 = 0.3; // Support/Resistance Weight
        private double buyThreshold = 0.6;
        private double sellThreshold = -0.2;
    }

    @Override
    public String getId() {
        return "S4";
    }

    @Override
    public String getName() {
        return "S4 - 앙상블 (Ensemble)";
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

        // Evaluate inputs
        Signal sig1 = s1.evaluate(event, context, s1.getDefaultParams()); // Using default params for components for now
        Signal sig2 = s2.evaluate(event, context, s2.getDefaultParams());
        Signal sig3 = s3.evaluate(event, context, s3.getDefaultParams());

        double score1 = getScore(sig1);
        double score2 = getScore(sig2);
        double score3 = getScore(sig3);

        double finalScore = (score1 * params.w1) + (score2 * params.w2) + (score3 * params.w3);

        // Logging Components (e.g. to extraData or Signal reason)
        String reasoning = String.format("S1:%.1f, S2:%.1f, S3:%.1f -> Final:%.2f", score1, score2, score3, finalScore);

        if (finalScore >= params.buyThreshold && !context.isHasPosition()) {
            return Signal.builder()
                    .type(Signal.Type.BUY)
                    .strategyName(getName())
                    .reasonCode("ENSEMBLE_BUY")
                    .reasonMessageKo("전략 합산 점수 " + String.format("%.2f", finalScore) + " (S1:" + score1 + " S2:"
                            + score2 + " S3:" + score3 + ")")
                    .sizeFactor(1.0)
                    .confidence(finalScore)
                    .build();
        } else if (context.isHasPosition() && finalScore <= params.sellThreshold) {
            return Signal.builder()
                    .type(Signal.Type.SELL)
                    .strategyName(getName())
                    .reasonCode("ENSEMBLE_SELL")
                    .reasonMessageKo("전략 합산 점수 하락 (" + String.format("%.2f", finalScore) + ")")
                    .sizeFactor(1.0)
                    .build();
        }

        return Signal.none();
    }

    private double getScore(Signal signal) {
        if (signal.getType() == Signal.Type.BUY)
            return 1.0 * (signal.getConfidence() > 0 ? signal.getConfidence() : 0.5);
        if (signal.getType() == Signal.Type.SELL)
            return -1.0;
        return 0.0;
    }
}
