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

        // 1. Fetch Data - DB 우선, 없으면 API 호출
        List<com.antigravity.trading.domain.dto.CandleDto> candles = fetchCandleData(symbol, start, end);

        // 3. Create BacktestRun (for persistence, not directly used in simulation logic
        // anymore)
        BacktestRun run = BacktestRun.builder()
                .startedAt(LocalDateTime.now())
                .status("RUNNING")
                .paramsJson(
                        "{\"symbol\":\"" + symbol + "\", \"start\":\"" + start + "\", \"end\":\"" + end
                                + "\", \"strategyId\":\"" + strategyId + "\", \"params\":\"" + paramsJson + "\"}")
                .build();
        backtestRunRepository.save(run);

        // Calculate Indicators Pre-Loop
        List<BigDecimal> closes = candles.stream().map(com.antigravity.trading.domain.dto.CandleDto::getClose)
                .collect(Collectors.toList());
        List<Double> rsiValues = TechnicalIndicators.calculateRsi(closes, 14);

        // 3. Simulate Strategy
        com.antigravity.trading.strategy.v2.TradingStrategy strategy = strategyRegistry
                .getStrategy(strategyId != null ? strategyId : "S1");
        if (strategy == null)
            throw new IllegalArgumentException("Unknown Strategy ID: " + strategyId);

        BacktestResult result = simulateStrategy(run.getId(), symbol, candles, strategy, start, end, rsiValues,
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
            List<Double> rsiValues,
            String paramsJson) {
        BigDecimal balance = new BigDecimal("10000000");
        BigDecimal holdingQty = BigDecimal.ZERO;
        BigDecimal entryPrice = BigDecimal.ZERO;
        int tradeCount = 0;

        List<TradeRecord> trades = new ArrayList<>();
        Map<String, Integer> rejectionStats = new HashMap<>();

        List<BigDecimal> prices = new ArrayList<>();
        List<BigDecimal> volumes = new ArrayList<>();

        // Context
        StrategyContext context = StrategyContext.builder()
                .symbol(symbol)
                .history(candles) // V2: Inject History
                .hasPosition(false)
                .entryPrice(BigDecimal.ZERO)
                .quantity(0L)
                .availableCash(balance)
                .extraData(new HashMap<>())
                .build();

        com.antigravity.trading.strategy.v2.StrategyParams strategyParamsObj = null; // Parsed params

        // Parse paramsJson and put into context extraData if needed
        if (paramsJson != null) {
            // Simple approach: Store raw json, strategy parses it? Or generic map?
            // For now, assume S1 'mode' is passed as "mode":"LOOSE" in simple map logic
            // Better: Context has "params" object.
            // Let's use extraData for now.
            // If paramsJson is like {"mode":"LOOSE"}, we can parse it.
            // But for simplicity of this step, let's just put it as "paramsJson" string.
            context.getExtraData().put("paramsJson", paramsJson);
            // Also support S1 specific "mode" for legacy compatibility if paramsJson
            // contains it
            if (paramsJson.contains("LOOSE"))
                context.getExtraData().put("mode", "LOOSE");
        }

        for (int i = 0; i < candles.size(); i++) {
            com.antigravity.trading.domain.dto.CandleDto curr = candles.get(i);
            prices.add(curr.getClose());
            volumes.add(curr.getVolume());

            LocalDateTime dt = parseTime(curr.getTime());

            if (dt.isBefore(start) || dt.isAfter(end))
                continue;

            // Calc Indicators
            BigDecimal ma20 = null;
            boolean ma20Rising = false;
            BigDecimal ma60 = null;
            double volRatio = 1.0;
            BigDecimal twentyDayHigh = curr.getHigh(); // Default
            Double rsi = (i < rsiValues.size()) ? rsiValues.get(i) : null;

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

            // Update Context
            context.setHasPosition(holdingQty.compareTo(BigDecimal.ZERO) > 0);
            context.setEntryPrice(entryPrice);
            context.setQuantity(holdingQty.longValue());
            context.setAvailableCash(balance);
            context.setHighWaterMark(curr.getHigh());

            // Build MarketEvent
            MarketEvent event = MarketEvent.builder()
                    .symbol(symbol)
                    .timestamp(dt)
                    .currentPrice(curr.getClose())
                    .open(curr.getOpen())
                    .high(curr.getHigh())
                    .low(curr.getLow())
                    .close(curr.getClose())
                    .volume(curr.getVolume().longValue())
                    .ma20(ma20)
                    .ma60(ma60)
                    .isMa20Rising(ma20Rising)
                    .breakoutPrice(twentyDayHigh)
                    .volumeRatio(volRatio)
                    .rsi(rsi)
                    .build();

            // Execute Engine
            MarketEvent analysisEvent = event.toBuilder()
                    .currentPrice(curr.getHigh()) // Use High to test breakout potential
                    .timestamp(dt.withHour(10).withMinute(0))
                    .build();

            // V2: Parse Params once or per loop?
            // Parsing per loop is inefficient. Ideally parse outside loop.
            // But we need to do it once.
            // Better: Parse "paramsJson" at start of simulateStrategy.
            if (strategyParamsObj == null) {
                // Initialize params
                try {
                    strategyParamsObj = com.antigravity.trading.strategy.v2.StrategyParams.fromJson(paramsJson,
                            strategy.getParamsClass());
                } catch (Exception e) {
                    log.error("Failed to parse params", e);
                    strategyParamsObj = strategy.getDefaultParams();
                }
            }

            Signal signal = strategy.evaluate(analysisEvent, context, strategyParamsObj);

            // Log Decision
            if (signal.getType() != Signal.Type.NONE) {
                String inputDesc = String.format("P(H):%.0f, MA20:%.0f, VR:%.1f, Break:%.0f, RSI:%.1f",
                        curr.getHigh(), ma20 != null ? ma20.doubleValue() : 0, volRatio, twentyDayHigh.doubleValue(),
                        rsi != null ? rsi : 0);

                com.antigravity.trading.domain.entity.DecisionLog decisionLog = com.antigravity.trading.domain.entity.DecisionLog
                        .builder()
                        .traceId(UUID.randomUUID().toString())
                        .backtestRunId(runId)
                        .symbol(symbol)
                        .eventTime(dt)
                        .decisionType(signal.getType().name())
                        .reasonsJson(signal.getReasonCode() + ": " + signal.getReasonDetail())
                        .inputsJson(inputDesc)
                        .build();
                decisionLogRepository.save(decisionLog);

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
                    if (holdingQty.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal amount = holdingQty.multiply(curr.getClose());

                        BigDecimal pnl = curr.getClose().subtract(entryPrice)
                                .divide(entryPrice, 4, RoundingMode.HALF_UP)
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
            } else {
                // Collect Rejection Stats
                String reason = signal.getReasonCode();
                if (reason != null) {
                    rejectionStats.put(reason, rejectionStats.getOrDefault(reason, 0) + 1);
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
                .rejectionStats(rejectionStats)
                .build();
    }

    private LocalDateTime parseTime(String dateStr) {
        // Expects yyyy-MM-dd for Chart, converts to LocalDateTime for logic
        // dateStr = "2023-08-02"
        return LocalDateTime.parse(dateStr + " 15:30:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 캔들 데이터 조회 (DB 우선, 없으면 API 호출)
     */
    private List<com.antigravity.trading.domain.dto.CandleDto> fetchCandleData(
            String symbol, LocalDateTime start, LocalDateTime end) {

        // 1. DB에서 조회 시도
        List<com.antigravity.trading.domain.entity.CandleHistory> dbCandles = candleHistoryRepository
                .findBySymbolAndTimeBetween(symbol, start, end);

        if (dbCandles != null && !dbCandles.isEmpty()) {
            log.info("Loaded {} candles from DB for {}", dbCandles.size(), symbol);

            // DB → DTO 변환
            return dbCandles.stream()
                    .map(this::toCandleDtoFromEntity)
                    .sorted((a, b) -> a.getTime().compareTo(b.getTime()))
                    .toList();
        }

        // 2. DB에 데이터 없으면 API 호출 (폴백)
        log.warn("No data in DB for {} ({} ~ {}), falling back to KIS API", symbol, start, end);

        KisChartResponse response = kisApiClient.getDailyChart(symbol, start, end);
        List<KisChartResponse.Output2> rawCandles = response.getOutput2();

        if (rawCandles == null || rawCandles.isEmpty()) {
            throw new IllegalArgumentException("No data found for the given period (DB and API both empty)");
        }

        // 3. API → DTO 변환
        return rawCandles.stream()
                .map(this::toCandleDtoFromApi)
                .sorted((a, b) -> a.getTime().compareTo(b.getTime()))
                .toList();
    }

    /**
     * CandleHistory 엔티티를 CandleDto로 변환
     */
    private com.antigravity.trading.domain.dto.CandleDto toCandleDtoFromEntity(
            com.antigravity.trading.domain.entity.CandleHistory entity) {

        // LocalDateTime → "yyyy-MM-dd" 형식 문자열
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

    /**
     * KIS API 응답을 CandleDto로 변환 (기존 메서드 이름 변경)
     */
    private com.antigravity.trading.domain.dto.CandleDto toCandleDtoFromApi(
            KisChartResponse.Output2 output) {
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
