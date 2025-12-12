package com.antigravity.trading.domain.strategy;

import java.math.BigDecimal;

public interface TradingStrategy {
    String getName();

    StrategySignal analyze(String symbol,
            java.util.List<com.antigravity.trading.controller.CandleController.CandleDto> candles); // Simplified DTO
}
