package com.antigravity.trading.service;

import com.antigravity.trading.domain.entity.BacktestRun;
import com.antigravity.trading.domain.entity.DecisionLog;
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

import com.antigravity.trading.engine.StrategyRegistry;
import com.antigravity.trading.infrastructure.api.KisApiClient;
import com.antigravity.trading.infrastructure.api.dto.KisChartResponse;
import com.antigravity.trading.util.TechnicalIndicators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BacktestService {

    private final KisApiClient kisApiClient;
    private final StrategyRegistry strategyRegistry;
    private final ReasonCodeMapper reasonMapper;
    private final BacktestRunRepository backtestRunRepository;
    private final DecisionLogRepository decisionLogRepository;
    private final com.antigravity.trading.repository.CandleHistoryRepository candleHistoryRepository;

    @Autowired
    public BacktestService(KisApiClient kisApiClient, StrategyRegistry strategyRegistry, ReasonCodeMapper reasonMapper,
            BacktestRunRepository backtestRunRepository, DecisionLogRepository decisionLogRepository,
            com.antigravity.trading.repository.CandleHistoryRepository candleHistoryRepository) {
        this.kisApiClient = kisApiClient;
        this.strategyRegistry = strategyRegistry;
        this.reasonMapper = reasonMapper;
        this.backtestRunRepository = backtestRunRepository;
        this.decisionLogRepository = decisionLogRepository;
        this.candleHistoryRepository = candleHistoryRepository;
    }

    public BacktestResult runBacktest(String symbol, LocalDateTime startDate, LocalDateTime endDate) {
        return runBacktest(symbol, startDate, endDate, "S1", null);
    }

    public BacktestResult runBacktest(String symbol, LocalDateTime startDate, LocalDateTime endDate,
            String strategyMode) {
        return runBacktest(symbol, startDate, endDate, "S1", "{\"mode\":\"" + strategyMode + "\"}");
    }

    @Cacheable("backtest")
    public BacktestResult runBacktest(String symbol, LocalDateTime start, LocalDateTime end, String strategyId,
            String paramsJson) {
        long startTime = System.currentTimeMillis();
        log.info("Starting backtest for {} from {} to {} (Strategy: {}, Params: {})", symbol, start, end, strategyId,
                paramsJson);

        // 1. Fetch Data
        List<com.antigravity.trading.domain.dto.CandleDto> candles = fetchCandleData(symbol, start, end);

        // 2. Create Run Record
        BacktestRun run = BacktestRun.builder()
                .startedAt(LocalDateTime.now())
                .status("RUNNING")
                .paramsJson(
                        "{\"symbol\":\"" + symbol + "\", \"start\":\"" + start + "\", \"end\":\"" + end
                                + "\", \"strategyId\":\"" + strategyId + "\", \"params\":\"" + paramsJson + "\"}")
                .build();
        backtestRunRepository.save(run);

        // 3. Select Strategy
        com.antigravity.trading.strategy.v2.TradingStrategy strategy = strategyRegistry
                .getStrategy(strategyId != null ? strategyId : "S1");
        if (strategy == null)
            throw new IllegalArgumentException("Unknown Strategy ID: " + strategyId);

        // 4. Simulate
        BacktestResult result = simulateStrategy(run.getId(), symbol, candles, strategy, start, end, null,
                paramsJson);

        run.setEndedAt(LocalDateTime.now());
        run.setStatus("COMPLETED");
        run.setSummaryJson(
                "{\"finalBalance\":" + result.getFinalBalance() + ", \"trades\":" + result.getTotalTrades() + "}");
        backtestRunRepository.save(run);

        return result;
    }

    private BacktestResult simulateStrategy(Long runId, String symbol,
            List<com.antigravity.trading.domain.dto.CandleDto> candles,
            com.antigravity.trading.strategy.v2.TradingStrategy strategy, LocalDateTime start, LocalDateTime end,
            List<Double> inputRsiValues,
            String paramsJson) {
        BigDecimal balance = new BigDecimal("10000000");
        BigDecimal holdingQty = BigDecimal.ZERO;
        BigDecimal entryPrice = BigDecimal.ZERO;
        int tradeCount = 0;

        List<TradeRecord> trades = new ArrayList<>();
        Map<String, Integer> rejectionStats = new HashMap<>();

        // Context Setup
        StrategyContext context = StrategyContext.builder()
                .symbol(symbol)
                .history(candles)
                .hasPosition(false)
                .entryPrice(BigDecimal.ZERO)
                .quantity(0L)
                .dailyEntryCount(0)
                .availableCash(balance)
                .extraData(new HashMap<>())
                .build();

        // Parse Params
        com.antigravity.trading.strategy.v2.StrategyParams strategyParamsObj = null;
        if (paramsJson != null) {
            try {
                strategyParamsObj = com.antigravity.trading.strategy.v2.StrategyParams.fromJson(paramsJson,
                        strategy.getParamsClass());
            } catch (Exception e) {
                log.error("Param parse error", e);
            }
        }
        if (strategyParamsObj == null)
            strategyParamsObj = strategy.getDefaultParams();

        // Calculate Indicators (Upfront)
        List<BigDecimal> closes = candles.stream().map(com.antigravity.trading.domain.dto.CandleDto::getClose)
                .collect(Collectors.toList());
        List<BigDecimal> volumes = candles.stream().map(com.antigravity.trading.domain.dto.CandleDto::getVolume)
                .collect(Collectors.toList());

        List<BigDecimal> ma20List = TechnicalIndicators.calculateSma(closes, 20);
        List<BigDecimal> ma60List = TechnicalIndicators.calculateSma(closes, 60);
        List<Double> volRatioList = TechnicalIndicators.calculateVolumeRatio(volumes, 20);
        List<Double> atrList = TechnicalIndicators.calculateAtr(candles, 14);
        List<BigDecimal> donchianHighList = TechnicalIndicators.calculateDonchianHigh(candles, 20); // 20-day high
        List<BigDecimal> donchianLowList = TechnicalIndicators.calculateDonchianLow(candles, 20); // 20-day low

        List<Double> rsiList = (inputRsiValues != null && !inputRsiValues.isEmpty()) ? inputRsiValues
                : TechnicalIndicators.calculateRsi(closes, 14);

        // Simulation Loop
        for (int i = 0; i < candles.size(); i++) {
            com.antigravity.trading.domain.dto.CandleDto curr = candles.get(i);
            LocalDateTime dt = parseTime(curr.getTime());

            if (dt.isBefore(start) || dt.isAfter(end))
                continue;

            // Build MarketEvent using pre-calculated indicators
            // Caution: Donchian Breakout usually checks "High of Previous N Days".
            // If donchianHighList.get(i) includes 'i', then Price > Donchian is impossible.
            // TechnicalIndicators.calculateDonchianHigh implementation I wrote returns Max
            // in [i-period+1, i].
            // So get(i) includes current.
            // We need PREVIOUS day's Donchian High for breakout trigger.
            // So we use get(i-1).

            BigDecimal prevDonchianHigh = (i > 0) ? donchianHighList.get(i - 1) : curr.getHigh();
            BigDecimal prevDonchianLow = (i > 0) ? donchianLowList.get(i - 1) : curr.getLow();

            MarketEvent event = MarketEvent.builder()
                    .symbol(symbol)
                    .timestamp(dt)
                    .currentPrice(curr.getClose())
                    .open(curr.getOpen())
                    .high(curr.getHigh())
                    .low(curr.getLow())
                    .close(curr.getClose())
                    .volume(curr.getVolume().longValue())
                    .ma20(ma20List.get(i))
                    .ma60(ma60List.get(i))
                    .avgVol20(BigDecimal.ZERO) // Simplified/Unused
                    .volumeRatio(volRatioList.get(i))
                    .breakoutPrice(prevDonchianHigh) // Mapping Donchian High to breakoutPrice
                    .rsi(rsiList.get(i))
                    // Phase 14 Fields
                    .atr(atrList.get(i))
                    .donchianHigh(prevDonchianHigh)
                    .donchianLow(prevDonchianLow)
                    // Opening Range (Null for Daily)
                    .openingRangeHigh(null)
                    .openingRangeLow(null)
                    .build();

            // Context Update
            context.setHasPosition(holdingQty.compareTo(BigDecimal.ZERO) > 0);
            context.setEntryPrice(entryPrice);
            context.setQuantity(holdingQty.longValue());
            context.setAvailableCash(balance);
            context.setHighWaterMark(curr.getHigh());

            // Execute Strategy
            // We pretend it's 10:00 AM or just Daily Close decision?
            // S1-S4 are Daily Swing. Evaluation is usually at Close or on Intraday
            // Breakout.
            // If we use 'curr.getClose()' as price, it's Close-based.
            // S1 Donchian Breakout checks if Price > Donchian.
            // If we backtest on Daily Candles, we assume we buy if High > Donchian?
            // Realistically, if High > Donchian, we entered intraday.
            // To simulate Intraday Entry heavily, we need High/Low check.
            // But Signal evaluate() receives 'event' with 'currentPrice'.
            // If we pass 'close', it checks Close.
            // For backtesting Breakout on Daily Bars:
            // We should check if High > Breakout Level. If so, entry price =
            // max(BreakoutLevel, Open).
            // But 'evaluate' method returns boolean logic based on 'currentPrice'.
            // Simple Backtest: Pass 'Close'. Trend following usually OK with Close.
            // Breakout strategies might miss the "Intraday Breakout" if Close < Breakout
            // but High > Breakout.
            // Let's stick to Close for simplicity of V1/V2 migration unless explicitly
            // requested High-stop.
            // User S1: "Close > N-days High" (Confirmed Breakout) or "Intraday Breakout"?
            // User said: "Close > Recent N High" (Upward Breakout).
            // Actually usually Donchian is "Price breaks High".
            // I'll stick to Close for now.

            Signal signal = strategy.evaluate(event, context, strategyParamsObj);

            // Process Signal
            if (signal.getType() == Signal.Type.BUY) {
                if (holdingQty.compareTo(BigDecimal.ZERO) == 0) { // Only if flat
                    // Buy Logic
                    BigDecimal price = curr.getClose(); // Buy at Close
                    // Or Buy at Breakout Price (Stop Limit)?

                    BigDecimal alloc = balance.multiply(new BigDecimal("0.5")); // 50%
                    BigDecimal qty = alloc.divide(price, 0, RoundingMode.DOWN);

                    if (qty.compareTo(BigDecimal.ZERO) > 0) {
                        balance = balance.subtract(qty.multiply(price));
                        holdingQty = holdingQty.add(qty);
                        entryPrice = price;
                        tradeCount++;
                        trades.add(TradeRecord.builder()
                                .time(dt)
                                .type("BUY")
                                .price(price)
                                .quantity(qty)
                                .reason(signal.getReasonMessageKo() != null && !signal.getReasonMessageKo().isEmpty()
                                        ? signal.getReasonMessageKo()
                                        : signal.getReasonCode())
                                .pnlPercent(BigDecimal.ZERO)
                                .build());
                    }
                }
            } else if (signal.getType() == Signal.Type.SELL) {
                if (holdingQty.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal price = curr.getClose();
                    BigDecimal amount = holdingQty.multiply(price);
                    BigDecimal pnl = price.subtract(entryPrice).divide(entryPrice, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal(100));

                    balance = balance.add(amount);

                    trades.add(TradeRecord.builder()
                            .time(dt)
                            .type("SELL")
                            .price(price)
                            .quantity(holdingQty)
                            .reason(signal.getReasonMessageKo() != null && !signal.getReasonMessageKo().isEmpty()
                                    ? signal.getReasonMessageKo()
                                    : signal.getReasonCode())
                            .pnlPercent(pnl)
                            .build());

                    holdingQty = BigDecimal.ZERO;
                    entryPrice = BigDecimal.ZERO;
                    tradeCount++;
                }
            }
        }

        // Force Liquidate at End
        if (holdingQty.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal closePrice = candles.get(candles.size() - 1).getClose();
            balance = balance.add(holdingQty.multiply(closePrice));
            trades.add(TradeRecord.builder()
                    .time(parseTime(candles.get(candles.size() - 1).getTime()))
                    .type("SELL")
                    .price(closePrice)
                    .quantity(holdingQty)
                    .reason("만기 청산 (Force Liquidation)")
                    .pnlPercent(BigDecimal.ZERO)
                    .build());
        }

        BigDecimal finalReturn = balance.subtract(new BigDecimal("10000000"))
                .divide(new BigDecimal("10000000"), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        return BacktestResult.builder().symbol(symbol).finalBalance(balance).totalReturnPercent(finalReturn)
                .totalTrades(trades.size()).trades(trades).candles(candles).rejectionStats(rejectionStats).build();
    }

    private LocalDateTime parseTime(String dateStr) {
        return LocalDateTime.parse(dateStr + " 15:30:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private List<com.antigravity.trading.domain.dto.CandleDto> fetchCandleData(String symbol, LocalDateTime start,
            LocalDateTime end) {
        List<com.antigravity.trading.domain.entity.CandleHistory> dbCandles = candleHistoryRepository
                .findBySymbolAndTimeBetween(symbol, start, end);
        if (dbCandles != null && !dbCandles.isEmpty()) {
            return dbCandles.stream().map(this::toCandleDtoFromEntity)
                    .sorted((a, b) -> a.getTime().compareTo(b.getTime())).collect(Collectors.toList());
        }
        try {
            KisChartResponse response = kisApiClient.getDailyChart(symbol, start, end);
            if (response.getOutput2() == null)
                return new ArrayList<>();
            return response.getOutput2().stream().map(this::toCandleDtoFromApi)
                    .sorted((a, b) -> a.getTime().compareTo(b.getTime())).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("API Fetch Error", e);
            return new ArrayList<>();
        }
    }

    private com.antigravity.trading.domain.dto.CandleDto toCandleDtoFromEntity(
            com.antigravity.trading.domain.entity.CandleHistory entity) {
        String timeStr = entity.getTime().toLocalDate().toString();
        return com.antigravity.trading.domain.dto.CandleDto.builder()
                .time(timeStr)
                .open(entity.getOpen())
                .high(entity.getHigh())
                .low(entity.getLow())
                .close(entity.getClose())
                .volume(entity.getVolume() != null ? new BigDecimal(entity.getVolume()) : BigDecimal.ZERO)
                .build();
    }

    private com.antigravity.trading.domain.dto.CandleDto toCandleDtoFromApi(KisChartResponse.Output2 output) {
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
        private java.util.Map<String, Integer> rejectionStats;
    }

    @Getter
    @Builder
    public static class TradeRecord {
        private LocalDateTime time;
        private String type;
        private BigDecimal price;
        private BigDecimal quantity;
        private String reason;
        private BigDecimal pnlPercent;
    }
}
