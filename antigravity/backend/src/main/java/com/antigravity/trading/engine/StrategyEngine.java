package com.antigravity.trading.engine;

import com.antigravity.trading.engine.model.MarketEvent;
import com.antigravity.trading.engine.model.Signal;
import com.antigravity.trading.engine.model.StrategyContext;

public interface StrategyEngine {
    /**
     * Analyze market data and current context to generate a trading signal.
     * This must be deterministic (stateless except for Context).
     * 
     * @param event   Current market snapshot (price, indicators)
     * @param context Account/Position state
     * @return Signal (BUY, SELL, HOLD, or NONE)
     */
    Signal analyze(MarketEvent event, StrategyContext context);

    /**
     * @return Unique strategy name (e.g. TrendMomentumScalpV1)
     */
    String getName();
}
