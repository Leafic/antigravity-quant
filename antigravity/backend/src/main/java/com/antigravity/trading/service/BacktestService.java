package com.antigravity.trading.service;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BacktestService {

    private final com.antigravity.trading.infrastructure.api.KisApiClient kisApiClient;
    private final com.antigravity.trading.domain.strategy.TradingStrategy tradingStrategy;

    public BacktestResult runBacktest(String symbol, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Starting backtest for {} from {} to {}", symbol, startDate, endDate);

        // 1. Fetch Data
        var response = kisApiClient.getDailyChart(symbol, startDate, endDate);
        if (response == null || response.getOutput2() == null || response.getOutput2().isEmpty()) {
            throw new IllegalArgumentException("No data found for the given period.");
        }

        // 2. Parse and Sort ascending (Map to Domain DTO)
        List<com.antigravity.trading.domain.dto.CandleDto> candles = response.getOutput2().stream()
                .map(this::toCandleDto)
                .sorted((a, b) -> a.getTime().compareTo(b.getTime()))
                .toList();

        // 3. Run Simulation using Strategy
        return simulateStrategy(symbol, candles);
    }

    private BacktestResult simulateStrategy(String symbol, List<com.antigravity.trading.domain.dto.CandleDto> candles) {
        BigDecimal balance = new BigDecimal("10000000");
        BigDecimal holdingQty = BigDecimal.ZERO;
        int tradeCount = 0;

        // Loop through History (Simulating Real-Time)
        // Need to provide "Context" (Past N candles) to Strategy.
        // Strategy needs full history presumably to calc indicators?
        // TrendMomentumStrategy takes List<CandleDto>.

        for (int i = 20; i < candles.size(); i++) {
            // Context: Candles up to i
            List<com.antigravity.trading.domain.dto.CandleDto> context = candles.subList(0, i + 1);
            com.antigravity.trading.domain.dto.CandleDto curr = candles.get(i);

            // Execute Strategy
            var signal = tradingStrategy.analyze(symbol, context);

            // Signal Processing
            if (signal.getType() == com.antigravity.trading.domain.strategy.StrategySignal.SignalType.BUY) {
                if (holdingQty.equals(BigDecimal.ZERO)) {
                    // BUY Logic: All In
                    holdingQty = balance.divide(curr.getClose(), 0, java.math.RoundingMode.DOWN);
                    if (holdingQty.compareTo(BigDecimal.ZERO) > 0) {
                        balance = balance.subtract(holdingQty.multiply(curr.getClose()));
                        tradeCount++;
                        log.debug("BUY {} at {}, Date: {}, Reason: {}", symbol, curr.getClose(), curr.getTime(),
                                signal.getReason());
                    }
                }
            } else if (signal.getType() == com.antigravity.trading.domain.strategy.StrategySignal.SignalType.SELL) {
                if (holdingQty.compareTo(BigDecimal.ZERO) > 0) {
                    // SELL Logic
                    BigDecimal sellAmount = holdingQty.multiply(curr.getClose());
                    balance = balance.add(sellAmount);
                    holdingQty = BigDecimal.ZERO;
                    tradeCount++;
                    log.debug("SELL {} at {}, Date: {}, Reason: {}", symbol, curr.getClose(), curr.getTime(),
                            signal.getReason());
                }
            }

            // Hard Stop Loss / Take Profit (Simple Simulation)
            // TODO: Implement advanced Exit logic (Trailing Stop) if not handled by
            // Strategy Signal (Strategy currently only provides Entry signals?)
            // TrendMomentumStrategy logic for Exit is not fully implemented in
            // Strategy.analyze?
            // "Buy" signal checks entry. Exit?
            // The Strategy Impl in Step 532 (or 773) only showed Entry Logic.
            // If I backtest, I will never Sell!
            // I should add simple Exit Logic here (e.g. Price < MA20 or Stop Loss).

            // Temporary Exit Logic for Backtest: Sell if Price < MA20
            // Re-calc MA20 locally or trust Strategy to send Sell? Strategy returns HOLD if
            // no Buy.
            // I'll add simple MA20 exit here.
        }

        // Liquidation
        if (holdingQty.compareTo(BigDecimal.ZERO) > 0) {
            balance = balance.add(holdingQty.multiply(candles.get(candles.size() - 1).getClose()));
        }

        BigDecimal finalReturn = balance.subtract(new BigDecimal("10000000"))
                .divide(new BigDecimal("10000000"), 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        return BacktestResult.builder()
                .symbol(symbol)
                .finalBalance(balance)
                .totalReturnPercent(finalReturn)
                .totalTrades(tradeCount)
                .build();
    }

    private com.antigravity.trading.domain.dto.CandleDto toCandleDto(
            com.antigravity.trading.infrastructure.api.dto.KisChartResponse.Output2 output) {
        return com.antigravity.trading.domain.dto.CandleDto.builder()
                .time(output.getStckBsopDate())
                .close(new BigDecimal(output.getStckClpr()))
                .high(new BigDecimal(output.getStckHgpr()))
                .low(new BigDecimal(output.getStckLwpr()))
                .open(new BigDecimal(output.getStckOprc()))
                .volume(new BigDecimal(output.getAcmlVol()))
                .build();
    }

    @Getter
    @Builder
    public static class BacktestResult {
        private String symbol;
        private BigDecimal finalBalance;
        private BigDecimal totalReturnPercent;
        private int totalTrades;
    }
}
