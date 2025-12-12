package com.antigravity.trading.service;

import com.antigravity.trading.domain.entity.CandleHistory;
import com.antigravity.trading.repository.CandleHistoryRepository;
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

    private final CandleHistoryRepository candleHistoryRepository;

    public BacktestResult runBacktest(String symbol, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Starting backtest for {} from {} to {}", symbol, startDate, endDate);

        List<CandleHistory> candles = candleHistoryRepository.findBySymbolAndTimeBetween(symbol, startDate, endDate);
        if (candles.isEmpty()) {
            throw new IllegalArgumentException("No data found for the given period.");
        }

        BigDecimal balance = new BigDecimal("10000000"); // 1000만원 시작
        BigDecimal holdingQty = BigDecimal.ZERO;
        int tradeCount = 0;
        int winCount = 0;

        // Simple Strategy: Golden Cross (MA20 > MA60 Buy, MA20 < MA60 Sell)
        // Note: This assumes candles are sorted by time.
        for (int i = 1; i < candles.size(); i++) {
            CandleHistory prev = candles.get(i - 1);
            CandleHistory curr = candles.get(i);

            if (prev.getMa20() == null || prev.getMa60() == null || curr.getMa20() == null || curr.getMa60() == null) {
                continue;
            }

            boolean prevBull = prev.getMa20().compareTo(prev.getMa60()) > 0;
            boolean currBull = curr.getMa20().compareTo(curr.getMa60()) > 0;

            // Buy Signal (Golden Cross)
            if (!prevBull && currBull && holdingQty.equals(BigDecimal.ZERO)) {
                // All in
                holdingQty = balance.divide(curr.getClose(), 0, java.math.RoundingMode.DOWN);
                balance = balance.subtract(holdingQty.multiply(curr.getClose()));
                tradeCount++;
                log.debug("BUY at {}", curr.getClose());
            }
            // Sell Signal (Dead Cross)
            else if (prevBull && !currBull && holdingQty.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal sellAmount = holdingQty.multiply(curr.getClose());
                BigDecimal profit = sellAmount.subtract(new BigDecimal("10000000")); // Simplified profit calc for this
                                                                                     // trade not cumulative here
                                                                                     // accurately for win rate but good
                                                                                     // enough for P/L

                if (sellAmount.compareTo(new BigDecimal("10000000")) > 0)
                    winCount++; // Very rough approximation for win count logic fix later

                balance = balance.add(sellAmount);
                holdingQty = BigDecimal.ZERO;
                tradeCount++;
                log.debug("SELL at {}", curr.getClose());
            }
        }

        // Final Liquidation
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

    @Getter
    @Builder
    public static class BacktestResult {
        private String symbol;
        private BigDecimal finalBalance;
        private BigDecimal totalReturnPercent;
        private int totalTrades;
    }
}
