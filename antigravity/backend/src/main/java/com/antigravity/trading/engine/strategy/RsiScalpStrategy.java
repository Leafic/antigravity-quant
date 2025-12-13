package com.antigravity.trading.engine.strategy;

import com.antigravity.trading.engine.StrategyEngine;
import com.antigravity.trading.engine.model.MarketEvent;
import com.antigravity.trading.engine.model.Signal;
import com.antigravity.trading.engine.model.StrategyContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Component
public class RsiScalpStrategy implements StrategyEngine {

    @Override
    public String getId() {
        return "S2";
    }

    @Override
    public String getName() {
        return "2) RSI 스캘핑 (과매수/과매도)";
    }

    @Override
    public String getDescription() {
        return "RSI 과매도(<30) 매수, 과매수(>70) 매도 전략";
    }

    @Override
    public String getDefaultParamsJson() {
        return "{\"rsiPeriod\":14, \"buyThreshold\":30, \"sellThreshold\":70}";
    }

    @Override
    public Signal analyze(MarketEvent event, StrategyContext context) {
        if (event.getRsi() == null) {
            return Signal.builder().type(Signal.Type.NONE).reasonCode("DATA_MISSING").build();
        }

        double rsi = event.getRsi();

        // Exit Logic
        if (context.isHasPosition()) {
            // Take Profit if RSI > 70
            if (rsi >= 70) {
                return Signal.builder().type(Signal.Type.SELL).reasonCode("RSI_OVERBOUGHT_SELL")
                        .reasonDetail("RSI " + rsi + " >= 70").build();
            }
            // Stop Loss logic (Common?) - Let's implement simple 2% stop here too?
            BigDecimal currentPrice = event.getCurrentPrice();
            BigDecimal entryPrice = context.getEntryPrice();
            if (currentPrice.subtract(entryPrice).divide(entryPrice, 4, RoundingMode.HALF_UP).doubleValue() < -0.02) {
                return Signal.builder().type(Signal.Type.SELL).reasonCode("STOP_LOSS").reasonDetail("Loss > 2%")
                        .build();
            }
            return Signal.builder().type(Signal.Type.HOLD).build();
        }

        // Entry Logic
        if (rsi <= 30) {
            return Signal.builder()
                    .type(Signal.Type.BUY)
                    .strategyName(getName())
                    .reasonCode("RSI_OVERSOLD_BUY")
                    .reasonDetail("RSI " + rsi + " <= 30")
                    .build();
        }

        return Signal.builder().type(Signal.Type.NONE).reasonCode("RSI_NEUTRAL").build();
    }
}
