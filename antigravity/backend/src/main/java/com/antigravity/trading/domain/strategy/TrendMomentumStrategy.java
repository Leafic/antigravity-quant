package com.antigravity.trading.domain.strategy;

import com.antigravity.trading.controller.CandleController;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;

@Component
public class TrendMomentumStrategy implements TradingStrategy {

    @Override
    public String getName() {
        return "TrendMomentumV1";
    }

    @Override
    public StrategySignal analyze(String symbol, List<CandleController.CandleDto> candles) {
        if (candles == null || candles.size() < 20) {
            return StrategySignal.builder().symbol(symbol).type(StrategySignal.SignalType.HOLD)
                    .reason("Insufficient Data").build();
        }

        CandleController.CandleDto current = candles.get(candles.size() - 1);

        // Note: Real indicators (MA20) should be pre-calculated or calculated here.
        // For skeleton: Return HOLD.
        // Step 2 will implement logic.

        return StrategySignal.builder()
                .symbol(symbol)
                .type(StrategySignal.SignalType.HOLD)
                .reason("Monitoring")
                .build();
    }
}
