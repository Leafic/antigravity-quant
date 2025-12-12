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

    public BacktestResult runBacktest(String symbol, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Starting backtest for {} from {} to {}", symbol, startDate, endDate);

        // 1. Fetch Data
        var response = kisApiClient.getDailyChart(symbol, startDate, endDate);
        if (response == null || response.getOutput2() == null || response.getOutput2().isEmpty()) {
            throw new IllegalArgumentException("No data found for the given period.");
        }

        // 2. Parse and Sort ascending
        // KIS returns DESC (latest first). We need ASC for simulation.
        List<BacktestCandle> candles = response.getOutput2().stream()
                .map(this::toBacktestCandle)
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .toList();

        // 3. Calculate Indicators (SMA 5, 20)
        calculateIndicators(candles);

        // 4. Run Simulation
        return simulateGoldenCross(symbol, candles);
    }

    // Internal Simulation Logic
    private BacktestResult simulateGoldenCross(String symbol, List<BacktestCandle> candles) {
        BigDecimal balance = new BigDecimal("10000000");
        BigDecimal holdingQty = BigDecimal.ZERO;
        int tradeCount = 0;
        int winCount = 0; // Not used yet

        // Loop
        for (int i = 1; i < candles.size(); i++) {
            BacktestCandle prev = candles.get(i - 1);
            BacktestCandle curr = candles.get(i);

            if (prev.getSma5() == null || prev.getSma20() == null ||
                    curr.getSma5() == null || curr.getSma20() == null) {
                continue;
            }

            // Cross Logic: SMA5 crosses above SMA20
            boolean prevBull = prev.getSma5().compareTo(prev.getSma20()) > 0;
            boolean currBull = curr.getSma5().compareTo(curr.getSma20()) > 0;

            // Buy (Golden Cross)
            if (!prevBull && currBull && holdingQty.equals(BigDecimal.ZERO)) {
                holdingQty = balance.divide(curr.getClose(), 0, java.math.RoundingMode.DOWN);
                if (holdingQty.compareTo(BigDecimal.ZERO) > 0) {
                    balance = balance.subtract(holdingQty.multiply(curr.getClose()));
                    tradeCount++;
                    log.debug("BUY {} at {}, Date: {}", symbol, curr.getClose(), curr.getDate());
                }
            }
            // Sell (Dead Cross)
            else if (prevBull && !currBull && holdingQty.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal sellAmount = holdingQty.multiply(curr.getClose());
                balance = balance.add(sellAmount);
                holdingQty = BigDecimal.ZERO;
                tradeCount++;
                log.debug("SELL {} at {}, Date: {}", symbol, curr.getClose(), curr.getDate());
            }
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

    private void calculateIndicators(List<BacktestCandle> candles) {
        for (int i = 0; i < candles.size(); i++) {
            // SMA 5
            if (i >= 4) {
                BigDecimal sum = BigDecimal.ZERO;
                for (int j = 0; j < 5; j++)
                    sum = sum.add(candles.get(i - j).getClose());
                candles.get(i).setSma5(sum.divide(new BigDecimal(5), 2, java.math.RoundingMode.HALF_UP));
            }
            // SMA 20
            if (i >= 19) {
                BigDecimal sum = BigDecimal.ZERO;
                for (int j = 0; j < 20; j++)
                    sum = sum.add(candles.get(i - j).getClose());
                candles.get(i).setSma20(sum.divide(new BigDecimal(20), 2, java.math.RoundingMode.HALF_UP));
            }
        }
    }

    private BacktestCandle toBacktestCandle(
            com.antigravity.trading.infrastructure.api.dto.KisChartResponse.Output2 output) {
        return BacktestCandle.builder()
                .date(output.getStckBsopDate())
                .close(new BigDecimal(output.getStckClpr()))
                .build();
    }

    @Getter
    @Builder
    @lombok.Setter
    private static class BacktestCandle {
        private String date;
        private BigDecimal close;
        private BigDecimal sma5;
        private BigDecimal sma20;
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
