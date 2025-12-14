package com.antigravity.trading.strategy.v2;

import com.antigravity.trading.engine.model.MarketEvent;
import com.antigravity.trading.engine.model.Signal;
import com.antigravity.trading.engine.model.StrategyContext;

public interface TradingStrategy {
    String getId();

    String getName();

    StrategyParams getDefaultParams();

    // V2 Evaluate
    Signal evaluate(MarketEvent event, StrategyContext context, StrategyParams params);

    Class<? extends StrategyParams> getParamsClass();
}
