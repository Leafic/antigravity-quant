package com.antigravity.trading.engine.strategy;

import com.antigravity.trading.engine.StrategyEngine;
import com.antigravity.trading.engine.model.MarketEvent;
import com.antigravity.trading.engine.model.Signal;
import com.antigravity.trading.engine.model.StrategyContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnsembleStrategy implements StrategyEngine {

    // Circular dependency risk if we inject Registry directly?
    // Using simple logic: Combine basic indicators manually or inject other beans
    // if needed.
    // For simplicity, I will implement logic based on Event Data directly
    // "Simulating" ensemble.
    // Or validly: Inject List<StrategyEngine> but filter self out to avoid
    // recursion?

    @Override
    public String getId() {
        return "S4";
    }

    @Override
    public String getName() {
        return "4) 앙상블 (Ensemble)";
    }

    @Override
    public String getDescription() {
        return "Trend + RSI + Volatility 종합 점수 기반 전략";
    }

    @Override
    public String getDefaultParamsJson() {
        return "{\"threshold\":0.7}";
    }

    @Override
    public Signal analyze(MarketEvent event, StrategyContext context) {
        // Scored Approach
        double score = 0.0;

        // 1. Trend (MA20)
        if (Boolean.TRUE.equals(event.isMa20Rising()))
            score += 20;

        // 2) RSI: Oversold (< 30) -> Bullish (+30), Overbought (> 70) -> Bearish (-30)
        Double rsi = event.getRsi();
        if (rsi != null) {
            if (rsi < 30)
                score += 30;
            else if (rsi > 70)
                score -= 30;
        }

        // 3) Volume: High Volume -> Confirmation (+10)
        if (event.getVolumeRatio() > 1.0)
            score += 0.4;

        // Decision
        if (context.isHasPosition()) {
            if (score < 0.3) {
                return Signal.builder().type(Signal.Type.SELL).reasonCode("ENSEMBLE_SELL")
                        .reasonDetail("Score dropped to " + score).build();
            }
            return Signal.builder().type(Signal.Type.HOLD).build();
        } else {
            if (score >= 0.7) {
                return Signal.builder()
                        .type(Signal.Type.BUY)
                        .strategyName(getName())
                        .reasonCode("ENSEMBLE_BUY")
                        .reasonDetail("Score " + score + " >= 0.7")
                        .confidence(score)
                        .build();
            }
        }

        return Signal.builder().type(Signal.Type.NONE).reasonCode("SCORE_LOW").reasonDetail("Score: " + score).build();
    }
}
