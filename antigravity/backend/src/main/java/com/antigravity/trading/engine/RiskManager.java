package com.antigravity.trading.engine;

import com.antigravity.trading.engine.model.Signal;
import com.antigravity.trading.engine.model.StrategyContext;

public interface RiskManager {
    /**
     * Validate if the generated signal is acceptable given account risk rules.
     * e.g. Daily Loss Limit, Max Position Count.
     * 
     * @param signal  Generated signal
     * @param context Current context
     * @return true if approved, false if rejected due to risk
     */
    boolean validate(Signal signal, StrategyContext context);

    String getRejectionReason();
}
