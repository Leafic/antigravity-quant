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
public class SupportResistanceStrategy implements StrategyEngine {

    @Override
    public String getId() {
        return "S3";
    }

    @Override
    public String getName() {
        return "3) 지지/저항 (S/R Breakout)";
    }

    @Override
    public String getDescription() {
        return "최근 20일 고점 돌파 매수, 20일 저점 붕괴 매도";
    }

    @Override
    public String getDefaultParamsJson() {
        return "{\"period\":20}";
    }

    @Override
    public Signal analyze(MarketEvent event, StrategyContext context) {
        // Assuming Breakout Price in Event is "N-Day High"
        if (event.getBreakoutPrice() == null) {
            return Signal.builder().type(Signal.Type.NONE).reasonCode("DATA_MISSING").build();
        }

        BigDecimal currentPrice = event.getCurrentPrice();
        BigDecimal resistance = event.getBreakoutPrice();

        // Exit Logic
        if (context.isHasPosition()) {
            // Simple trailing stop or fixed target
            BigDecimal entryPrice = context.getEntryPrice();
            if (currentPrice.subtract(entryPrice).divide(entryPrice, 4, RoundingMode.HALF_UP).doubleValue() < -0.03) {
                return Signal.builder().type(Signal.Type.SELL).reasonCode("STOP_LOSS").reasonDetail("Loss > 3%")
                        .build();
            }
            return Signal.builder().type(Signal.Type.HOLD).build();
        }

        // Entry Logic
        // Simple Breakout
        if (currentPrice.compareTo(resistance) > 0) {
            return Signal.builder()
                    .type(Signal.Type.BUY)
                    .strategyName(getName())
                    .reasonCode("SR_BREAKOUT_BUY")
                    .reasonDetail("Price " + currentPrice + " > Resistance " + resistance)
                    .build();
        }

        return Signal.builder().type(Signal.Type.NONE).reasonCode("NO_BREAKOUT").build();
    }
}
