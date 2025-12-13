package com.antigravity.trading.domain.strategy;

import java.math.BigDecimal;
import com.antigravity.trading.domain.dto.CandleDto;
import java.util.List;

public interface TradingStrategy {
    String getName();

    StrategySignal analyze(String symbol, List<CandleDto> candles);
}
