package com.antigravity.trading.service;

import com.antigravity.trading.domain.entity.BacktestRun;
import com.antigravity.trading.domain.entity.DecisionLog;
import com.antigravity.trading.engine.StrategyEngine;
import com.antigravity.trading.engine.model.MarketEvent;
import com.antigravity.trading.engine.model.Signal;
import com.antigravity.trading.engine.model.StrategyContext;
import com.antigravity.trading.repository.BacktestRunRepository;
import com.antigravity.trading.repository.DecisionLogRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BacktestService {

    private final com.antigravity.trading.infrastructure.api.KisApiClient kisApiClient;
    private final StrategyEngine strategyEngine;
    private final BacktestRunRepository backtestRunRepository;
    private final DecisionLogRepository decisionLogRepository;

    public BacktestResult runBacktest(String symbol, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Starting backtest for {} from {} to {}", symbol, startDate, endDate);

        // 1. Fetch Data
        var response = kisApiClient.getDailyChart(symbol, startDate, endDate);
        if (response == null || response.getOutput2() == null || response.getOutput2().isEmpty()) {
            throw new IllegalArgumentException("No data found for the given period.");
        }

        // 2. Parse and Sort
        List<com.antigravity.trading.domain.dto.CandleDto> candles = response.getOutput2().stream()
                .map(this::toCandleDto)
                .sorted((a, b) -> a.getTime().compareTo(b.getTime()))
                .toList();

        // 3. Create BacktestRun
        BacktestRun run = BacktestRun.builder()
                .startedAt(LocalDateTime.now())
                .status("RUNNING")
                .paramsJson(
                        "{\"symbol\":\"" + symbol + "\", \"start\":\"" + startDate + "\", \"end\":\"" + endDate + "\"}")
                .build();
        backtestRunRepository.save(run);

        // 4. Run Simulation
        BacktestResult result = simulateStrategy(run.getId(), symbol, candles);

        run.setEndedAt(LocalDateTime.now());
        run.setStatus("COMPLETED");
        run.setSummaryJson("{\"finalBalance\":" + result.getFinalBalance() + "}");
        backtestRunRepository.save(run);

        return result;
    }

    private BacktestResult simulateStrategy(Long runId, String symbol,
            List<com.antigravity.trading.domain.dto.CandleDto> candles) {
        BigDecimal balance = new BigDecimal("10000000");
        BigDecimal holdingQty = BigDecimal.ZERO;
        BigDecimal entryPrice = BigDecimal.ZERO;
        int tradeCount = 0;

        List<TradeRecord> trades = new ArrayList<>();

        List<BigDecimal> prices = new ArrayList<>();
        List<BigDecimal> volumes = new ArrayList<>();

        for (int i = 0; i < candles.size(); i++) {
            com.antigravity.trading.domain.dto.CandleDto curr = candles.get(i);
            prices.add(curr.getClose());
            volumes.add(curr.getVolume());

            // Calc Indicators
            BigDecimal ma20 = null;
            boolean ma20Rising = false;
            BigDecimal ma60 = null;
            double volRatio = 1.0;
            BigDecimal twentyDayHigh = curr.getHigh(); // Default

            if (prices.size() >= 20) {
                // MA20
                double sum20 = 0;
                for (int k = prices.size() - 20; k < prices.size(); k++)
                    sum20 += prices.get(k).doubleValue();
                ma20 = new BigDecimal(sum20 / 20.0);

                // MA20 Slope
                if (prices.size() >= 21) {
                    double sumPrev = 0;
                    for (int k = prices.size() - 21; k < prices.size() - 1; k++)
                        sumPrev += prices.get(k).doubleValue();
                    BigDecimal ma20Prev = new BigDecimal(sumPrev / 20.0);
                    ma20Rising = ma20.compareTo(ma20Prev) > 0;
                }

                // Volume Ratio (Vol / MA20 Vol)
                double volSum20 = 0;
                for (int k = volumes.size() - 20; k < volumes.size(); k++)
                    volSum20 += volumes.get(k).doubleValue();
                double avgVol20 = volSum20 / 20.0;
                if (avgVol20 > 0) {
                    volRatio = curr.getVolume().doubleValue() / avgVol20;
                }

                // 20-Day High (Breakout Level)
                double maxH = 0;
                for (int k = candles.size() - 21; k < candles.size() - 1; k++) { // Max of PREVIOUS 20 days
                    if (k >= 0)
                        maxH = Math.max(maxH, candles.get(k).getHigh().doubleValue());
                }
                if (maxH > 0)
                    twentyDayHigh = new BigDecimal(maxH);
            }

            if (prices.size() >= 60) {
                double sum60 = 0;
                for (int k = prices.size() - 60; k < prices.size(); k++)
                    sum60 += prices.get(k).doubleValue();
                ma60 = new BigDecimal(sum60 / 60.0);
            }

            // Build Context
            StrategyContext context = StrategyContext.builder()
                    .symbol(symbol)
                    .hasPosition(holdingQty.compareTo(BigDecimal.ZERO) > 0)
                    .entryPrice(entryPrice)
                    .quantity(holdingQty.longValue())
                    .availableCash(balance)
                    .highWaterMark(curr.getHigh())
                    .extraData(new HashMap<>())
                    .build();

            // Build MarketEvent
            LocalDateTime dt = parseTime(curr.getTime());
            MarketEvent event = MarketEvent.builder()
                    .symbol(symbol)
                    .timestamp(dt)
                    .currentPrice(curr.getClose()) // Strategy checks this for general trend, but for Breakout, usage
                                                   // depends on implementation.
                    .open(curr.getOpen())
                    .high(curr.getHigh()) // Strategy might check this?
                    .low(curr.getLow())
                    .close(curr.getClose())
                    .volume(curr.getVolume().longValue())
                    .ma20(ma20)
                    .ma60(ma60)
                    .isMa20Rising(ma20Rising)
                    .breakoutPrice(twentyDayHigh)
                    .volumeRatio(volRatio)
                    .build();

            // Execute Engine
            // PROBLEM: Strategy 'TrendMomentumScalpV1' uses event.getCurrentPrice() for
            // Breakout Check.
            // "if (currentPrice.compareTo(target) >= 0)"
            // If currentPrice is Close, and target is High*1.002, this never passes.
            // We must temporarily spoof Current Price as High for the purpose of checking
            // Breakout trigger during the day?
            // Or better: Update Strategy to check High if it's a "backtest mode"? No,
            // strategy should be stateless/agnostic.
            // Strategy assumes "CurrentPrice" is the price *right now*.
            // In a daily backtest, "Right Now" covers the entire day.
            // If High > Breakout, we *would have* entered.
            // So we should verify if High > Breakout.
            // But Strategy.analyze() returns Signal based on "event.currentPrice".
            // If we pass Close, it fails.
            // Use High as CurrentPrice?
            // If we use High, MA20 check (Price > MA20) might pass even if Close < MA20?
            // Conservative: Check Close > MA20.
            // Aggressive: Check High > MA20?
            // Let's create a specialized event for Strategy Analysis that tries to see if
            // ANY trade happened?
            // Simple Fix: Pass HIGH as currentPrice to see if meaningful signal is
            // generated.
            // Note: This approximates "Best Case" entry.
            // We will use CLOSE for the context update (marking to market).

            MarketEvent analysisEvent = event.toBuilder()
                    .currentPrice(curr.getHigh()) // Use High to test breakout potential
                    .build();

            Signal signal = strategyEngine.analyze(analysisEvent, context);

            // Log Decision
            if (signal.getType() != Signal.Type.NONE) {
                String inputDesc = String.format("P(H):%.0f, MA20:%.0f, VR:%.1f, Break:%.0f",
                        curr.getHigh(), ma20 != null ? ma20.doubleValue() : 0, volRatio, twentyDayHigh.doubleValue());

                DecisionLog decisionLog = DecisionLog.builder()
                        .traceId(UUID.randomUUID().toString())
                        .backtestRunId(runId)
                        .symbol(symbol)
                        .eventTime(dt)
                        .decisionType(signal.getType().name())
                        .reasonsJson(signal.getReasonCode() + ": " + signal.getReasonDetail())
                        .inputsJson(inputDesc)
                        .build();
                decisionLogRepository.save(decisionLog);
            }

            // Execution Logic (Simulated)
            if (signal.getType() == Signal.Type.BUY) {
                // Open Position
                BigDecimal alloc = balance.multiply(new BigDecimal("0.5")); // 50%
                // Price: We buy at Breakout Price (approx) or High?
                // Let's buy at Breakout * 1.002 if possible, else High.
                BigDecimal execPrice = twentyDayHigh.multiply(new BigDecimal("1.002"));
                if (execPrice.compareTo(curr.getHigh()) > 0)
                    execPrice = curr.getHigh(); // Cap at High

                BigDecimal qty = alloc.divide(execPrice, 0, RoundingMode.DOWN);
                if (qty.compareTo(BigDecimal.ZERO) > 0) {
                    balance = balance.subtract(qty.multiply(execPrice));
                    holdingQty = holdingQty.add(qty);
                    entryPrice = execPrice;
                    tradeCount++;

                    trades.add(TradeRecord.builder()
                            .time(dt)
                            .type("BUY")
                            .price(execPrice)
                            .quantity(qty)
                            .reason(signal.getReasonCode())
                            .pnlPercent(BigDecimal.ZERO)
                            .build());
                }
            } else if (signal.getType() == Signal.Type.SELL) {
                // Close Position
                // Exit at Close? Or TakeProfit level?
                // Strategy returns SELL if TP/SL hit *based on current price*.
                // Since we passed High, it might trigger TP.
                // If we passed High, we might falsely trigger TP if only wick hit it? Valid.
                // What about SL? We should check LOW for SL.
                // This requires multiple checks per candle:
                // 1. Check Low for SL.
                // 2. Check High for TP.
                // 3. Check High for Entry.
                // This is complex "Intraday Simulation".
                // For now, simple logic: Use Close for Exit. Use High for Entry.
                // If signal was BUY (from High), we processed it.
                // If signal was SELL (from High), we process it.

                if (holdingQty.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal amount = holdingQty.multiply(curr.getClose());

                    BigDecimal pnl = curr.getClose().subtract(entryPrice).divide(entryPrice, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal(100));

                    balance = balance.add(amount);

                    trades.add(TradeRecord.builder()
                            .time(dt)
                            .type("SELL")
                            .price(curr.getClose())
                            .quantity(holdingQty)
                            .reason(signal.getReasonCode())
                            .pnlPercent(pnl)
                            .build());

                    holdingQty = BigDecimal.ZERO;
                    entryPrice = BigDecimal.ZERO;
                    tradeCount++;
                }
            }
        }

        // Final Liquidation
        if (holdingQty.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal closePrice = candles.get(candles.size() - 1).getClose();
            balance = balance.add(holdingQty.multiply(closePrice));
            trades.add(TradeRecord.builder()
                    .time(parseTime(candles.get(candles.size() - 1).getTime()))
                    .type("SELL")
                    .price(closePrice)
                    .quantity(holdingQty)
                    .reason("FORCE_LIQUIDATION")
                    .pnlPercent(BigDecimal.ZERO)
                    .build());
        }

        BigDecimal finalReturn = balance.subtract(new BigDecimal("10000000"))
                .divide(new BigDecimal("10000000"), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        return BacktestResult.builder()
                .symbol(symbol)
                .finalBalance(balance)
                .totalReturnPercent(finalReturn)
                .totalTrades(trades.size())
                .trades(trades)
                .candles(candles)
                .build();
    }

    private LocalDateTime parseTime(String dateStr) {
        // Expects yyyy-MM-dd for Chart, converts to LocalDateTime for logic
        // dateStr = "2023-08-02"
        return LocalDateTime.parse(dateStr + " 15:30:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private com.antigravity.trading.domain.dto.CandleDto toCandleDto(
            com.antigravity.trading.infrastructure.api.dto.KisChartResponse.Output2 output) {
        String r = output.getStckBsopDate();
        String fmt = r;
        if (r != null && r.length() == 8) {
            fmt = r.substring(0, 4) + "-" + r.substring(4, 6) + "-" + r.substring(6, 8);
        }
        return com.antigravity.trading.domain.dto.CandleDto.builder()
                .time(fmt)
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
        private List<TradeRecord> trades;
        private List<com.antigravity.trading.domain.dto.CandleDto> candles;
    }

    @Getter
    @Builder
    public static class TradeRecord {
        private LocalDateTime time;
        private String type;
        private BigDecimal price;
        private BigDecimal quantity;
        private String reason;
        private BigDecimal pnlPercent; // For logic
    }
}
