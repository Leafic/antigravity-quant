package com.antigravity.trading.service;

import com.antigravity.trading.domain.entity.CandleHistory;
import com.antigravity.trading.domain.entity.StockMaster;
import com.antigravity.trading.domain.entity.ScheduledStock;
import com.antigravity.trading.infrastructure.api.KisApiClient;
import com.antigravity.trading.infrastructure.api.dto.KisChartResponse;
import com.antigravity.trading.repository.CandleHistoryRepository;
import com.antigravity.trading.repository.StockMasterRepository;
import com.antigravity.trading.repository.ScheduledStockRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 데이터 파이프라인 서비스
 * KIS API에서 일봉 데이터를 가져와 DB에 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataPipelineService {

    private final KisApiClient kisApiClient;
    private final CandleHistoryRepository candleHistoryRepository;
    private final StockMasterRepository stockMasterRepository;
    private final ScheduledStockRepository scheduledStockRepository;

    /**
     * 스케줄링 대상 종목의 일봉 데이터 수집 (활성화된 종목만)
     * @param days 수집할 일수 (기본: 100일)
     */
    public CollectionResult collectScheduledStocks(int days) {
        log.info("Starting data collection for SCHEDULED stocks (last {} days)", days);

        List<ScheduledStock> scheduledStocks = scheduledStockRepository.findByEnabledTrue();

        if (scheduledStocks.isEmpty()) {
            log.warn("No scheduled stocks found. Falling back to ALL stocks.");
            return collectAllStockData(days);
        }

        log.info("Found {} scheduled stocks to process", scheduledStocks.size());

        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);

        return collectStocksInRange(scheduledStocks, startDate, endDate);
    }

    /**
     * 스케줄링 대상 종목의 특정 기간 데이터 수집
     */
    public CollectionResult collectScheduledStocksInRange(LocalDate start, LocalDate end) {
        log.info("Starting data collection for SCHEDULED stocks from {} to {}", start, end);

        List<ScheduledStock> scheduledStocks = scheduledStockRepository.findByEnabledTrue();

        if (scheduledStocks.isEmpty()) {
            log.warn("No scheduled stocks found.");
            return CollectionResult.builder()
                .success(false)
                .totalStocks(0)
                .successCount(0)
                .failCount(0)
                .message("스케줄링된 종목이 없습니다.")
                .build();
        }

        log.info("Found {} scheduled stocks to process", scheduledStocks.size());

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(23, 59, 59);

        return collectStocksInRange(scheduledStocks, startDateTime, endDateTime);
    }

    /**
     * 종목 목록에 대해 특정 기간 데이터 수집 (공통 로직)
     */
    private CollectionResult collectStocksInRange(List<ScheduledStock> stocks, LocalDateTime startDate, LocalDateTime endDate) {
        int successCount = 0;
        int failCount = 0;
        int skippedCount = 0;
        int newDataCount = 0;
        List<String> processedSymbols = new ArrayList<>();
        List<String> failedSymbols = new ArrayList<>();

        for (ScheduledStock stock : stocks) {
            try {
                SingleStockResult result = collectSingleStockData(stock.getSymbol(), startDate, endDate);

                if (result.isSuccess()) {
                    successCount++;
                    newDataCount += result.getNewRecords();
                    processedSymbols.add(String.format("%s(%s) - 신규 %d건",
                        stock.getSymbol(), stock.getName(), result.getNewRecords()));

                    log.info("✓ {} ({}) - 신규: {}건, 스킵: {}건",
                        stock.getName(), stock.getSymbol(),
                        result.getNewRecords(), result.getSkippedRecords());
                } else {
                    failCount++;
                    failedSymbols.add(stock.getSymbol() + "(" + result.getMessage() + ")");
                }

                // Rate limit protection (KIS API 제한 고려)
                Thread.sleep(200);

            } catch (Exception e) {
                failCount++;
                failedSymbols.add(stock.getSymbol() + "(" + e.getMessage() + ")");
                log.error("✗ Failed to collect data for {} ({}): {}",
                    stock.getName(), stock.getSymbol(), e.getMessage());
            }
        }

        String message = String.format("수집 완료 - 성공: %d, 실패: %d, 신규 데이터: %d건",
            successCount, failCount, newDataCount);

        log.info(message);

        return CollectionResult.builder()
            .success(failCount == 0)
            .totalStocks(stocks.size())
            .successCount(successCount)
            .failCount(failCount)
            .newDataCount(newDataCount)
            .startDate(startDate)
            .endDate(endDate)
            .processedSymbols(processedSymbols)
            .failedSymbols(failedSymbols)
            .message(message)
            .build();
    }

    /**
     * 모든 종목의 일봉 데이터 수집 (관리자 전용)
     * @param days 수집할 일수 (기본: 100일)
     */
    public CollectionResult collectAllStockData(int days) {
        log.info("Starting data collection for ALL stocks (last {} days)", days);

        List<StockMaster> stocks = stockMasterRepository.findAll();
        log.info("Found {} stocks to process", stocks.size());

        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);

        int successCount = 0;
        int failCount = 0;
        int newDataCount = 0;
        List<String> processedSymbols = new ArrayList<>();
        List<String> failedSymbols = new ArrayList<>();

        for (StockMaster stock : stocks) {
            try {
                SingleStockResult result = collectSingleStockData(stock.getCode(), startDate, endDate);

                if (result.isSuccess()) {
                    successCount++;
                    newDataCount += result.getNewRecords();
                    processedSymbols.add(stock.getCode() + "(" + stock.getName() + ")");
                }

                // Rate limit protection
                Thread.sleep(200);

            } catch (Exception e) {
                failCount++;
                failedSymbols.add(stock.getCode() + "(" + e.getMessage() + ")");
                log.error("✗ Failed to collect data for {} ({}): {}",
                    stock.getName(), stock.getCode(), e.getMessage());
            }
        }

        log.info("Data collection completed. Success: {}, Failed: {}, New Data: {}",
            successCount, failCount, newDataCount);

        return CollectionResult.builder()
            .success(failCount == 0)
            .totalStocks(stocks.size())
            .successCount(successCount)
            .failCount(failCount)
            .newDataCount(newDataCount)
            .startDate(startDate)
            .endDate(endDate)
            .processedSymbols(processedSymbols)
            .failedSymbols(failedSymbols)
            .build();
    }

    private static final int MAX_DAYS_PER_REQUEST = 100;  // KIS API 최대 100일 제한

    /**
     * 단일 종목 데이터 수집 (100일 단위로 분할하여 수집)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SingleStockResult collectSingleStockData(String symbol, LocalDateTime startDate, LocalDateTime endDate) {
        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate.toLocalDate(), endDate.toLocalDate());

        // 100일 이하면 단일 요청
        if (totalDays <= MAX_DAYS_PER_REQUEST) {
            return collectSingleStockDataChunk(symbol, startDate, endDate);
        }

        // 100일 초과면 분할 수집
        log.info("Splitting {} days into chunks of {} days for {}", totalDays, MAX_DAYS_PER_REQUEST, symbol);

        int totalNewRecords = 0;
        int totalSkippedRecords = 0;
        List<String> errors = new ArrayList<>();

        LocalDateTime chunkStart = startDate;
        while (chunkStart.isBefore(endDate)) {
            LocalDateTime chunkEnd = chunkStart.plusDays(MAX_DAYS_PER_REQUEST);
            if (chunkEnd.isAfter(endDate)) {
                chunkEnd = endDate;
            }

            log.debug("Collecting chunk: {} ~ {}", chunkStart.toLocalDate(), chunkEnd.toLocalDate());

            try {
                SingleStockResult chunkResult = collectSingleStockDataChunk(symbol, chunkStart, chunkEnd);

                if (chunkResult.isSuccess()) {
                    totalNewRecords += chunkResult.getNewRecords();
                    totalSkippedRecords += chunkResult.getSkippedRecords();
                } else {
                    errors.add(String.format("%s~%s: %s",
                        chunkStart.toLocalDate(), chunkEnd.toLocalDate(), chunkResult.getMessage()));
                }

                // Rate limit protection (KIS API 제한 고려)
                Thread.sleep(200);

            } catch (Exception e) {
                errors.add(String.format("%s~%s: %s",
                    chunkStart.toLocalDate(), chunkEnd.toLocalDate(), e.getMessage()));
            }

            chunkStart = chunkEnd.plusDays(1);
        }

        String message = errors.isEmpty() ? "성공" : "일부 실패: " + String.join(", ", errors);

        return SingleStockResult.builder()
            .success(errors.isEmpty())
            .newRecords(totalNewRecords)
            .skippedRecords(totalSkippedRecords)
            .message(message)
            .build();
    }

    /**
     * 단일 종목 데이터 수집 - 단일 청크 (최대 100일)
     */
    private SingleStockResult collectSingleStockDataChunk(String symbol, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Collecting data chunk for {} from {} to {}", symbol, startDate, endDate);

        try {
            // 기존에 저장된 날짜 목록 조회
            Set<LocalDate> existingDates = candleHistoryRepository.findDistinctDatesBySymbol(symbol);
            log.debug("Existing dates for {}: {} days", symbol, existingDates.size());

            // KIS API에서 데이터 가져오기
            KisChartResponse response = kisApiClient.getDailyChart(symbol, startDate, endDate);

            if (response == null || response.getOutput2() == null || response.getOutput2().isEmpty()) {
                log.warn("No data received from KIS API for {} ({} ~ {})", symbol, startDate.toLocalDate(), endDate.toLocalDate());
                return SingleStockResult.builder()
                    .success(true)
                    .newRecords(0)
                    .skippedRecords(0)
                    .message("API에서 데이터 없음")
                    .build();
            }

            List<CandleHistory> newCandles = new ArrayList<>();
            int skippedCount = 0;

            for (KisChartResponse.Output2 output : response.getOutput2()) {
                // 날짜 파싱
                String dateStr = output.getStckBsopDate();
                LocalDate candleDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));

                // 이미 존재하는 날짜면 스킵
                if (existingDates.contains(candleDate)) {
                    skippedCount++;
                    continue;
                }

                CandleHistory candle = convertToCandle(symbol, output);

                // 디버그: 데이터 검증
                log.debug("Saving candle - symbol: {}, date: {}, close: {}",
                    candle.getSymbol(), candle.getTime().toLocalDate(), candle.getClose());

                newCandles.add(candle);
            }

            // 신규 데이터만 저장
            if (!newCandles.isEmpty()) {
                candleHistoryRepository.saveAll(newCandles);
                log.info("Saved {} new candles for {} (skipped {} existing)",
                    newCandles.size(), symbol, skippedCount);
            } else {
                log.debug("No new data to save for {} (all {} records already exist)",
                    symbol, skippedCount);
            }

            return SingleStockResult.builder()
                .success(true)
                .newRecords(newCandles.size())
                .skippedRecords(skippedCount)
                .message("성공")
                .build();

        } catch (Exception e) {
            log.error("Error collecting data for {}: {}", symbol, e.getMessage());
            return SingleStockResult.builder()
                .success(false)
                .newRecords(0)
                .skippedRecords(0)
                .message(e.getMessage())
                .build();
        }
    }

    /**
     * 특정 종목의 일봉 데이터 수집 (레거시 호환용)
     */
    @Transactional
    public void collectStockData(String symbol, int days) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);
        collectSingleStockData(symbol, startDate, endDate);
    }

    /**
     * 특정 기간의 데이터만 수집 (백필용)
     */
    @Transactional
    public void collectStockDataRange(String symbol, LocalDateTime start, LocalDateTime end) {
        collectSingleStockData(symbol, start, end);
    }

    /**
     * 종목별 데이터 현황 조회 (갭 감지 및 신뢰도 포함)
     */
    @Transactional(readOnly = true)
    public StockDataStatus getStockDataStatus(String symbol) {
        LocalDateTime minTime = candleHistoryRepository.findMinTimeBySymbol(symbol);
        LocalDateTime maxTime = candleHistoryRepository.findMaxTimeBySymbol(symbol);
        long totalCount = candleHistoryRepository.countBySymbol(symbol);
        Set<LocalDate> existingDates = candleHistoryRepository.findDistinctDatesBySymbol(symbol);

        // 갭(빠진 거래일) 감지
        List<LocalDate> missingDates = new ArrayList<>();
        int expectedTradingDays = 0;
        double completenessRate = 0.0;
        String reliabilityLevel = "UNKNOWN";

        if (minTime != null && maxTime != null) {
            missingDates = findMissingTradingDays(existingDates, minTime.toLocalDate(), maxTime.toLocalDate());

            // 예상 거래일 수 계산 (주말 제외)
            expectedTradingDays = countTradingDays(minTime.toLocalDate(), maxTime.toLocalDate());

            // 완결성 비율 계산
            if (expectedTradingDays > 0) {
                completenessRate = ((double) (expectedTradingDays - missingDates.size()) / expectedTradingDays) * 100;
            }

            // 신뢰도 레벨 결정
            if (completenessRate >= 99.0) {
                reliabilityLevel = "HIGH";      // 99% 이상: 높은 신뢰도
            } else if (completenessRate >= 95.0) {
                reliabilityLevel = "MEDIUM";    // 95-99%: 중간 신뢰도
            } else if (completenessRate >= 80.0) {
                reliabilityLevel = "LOW";       // 80-95%: 낮은 신뢰도
            } else {
                reliabilityLevel = "UNRELIABLE"; // 80% 미만: 신뢰 불가
            }
        }

        return StockDataStatus.builder()
            .symbol(symbol)
            .hasData(minTime != null)
            .minDate(minTime != null ? minTime.toLocalDate() : null)
            .maxDate(maxTime != null ? maxTime.toLocalDate() : null)
            .totalDays((int) totalCount)
            .existingDates(existingDates)
            .missingDates(missingDates)
            .hasGaps(!missingDates.isEmpty())
            .gapCount(missingDates.size())
            .expectedTradingDays(expectedTradingDays)
            .completenessRate(Math.round(completenessRate * 100.0) / 100.0)  // 소수점 2자리
            .reliabilityLevel(reliabilityLevel)
            .build();
    }

    /**
     * 두 날짜 사이의 거래일 수 계산 (주말 제외)
     */
    private int countTradingDays(LocalDate startDate, LocalDate endDate) {
        int count = 0;
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            java.time.DayOfWeek dayOfWeek = current.getDayOfWeek();
            boolean isWeekend = dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY;

            if (!isWeekend) {
                count++;
            }
            current = current.plusDays(1);
        }

        return count;
    }

    /**
     * 빠진 거래일 찾기 (주말 제외)
     */
    private List<LocalDate> findMissingTradingDays(Set<LocalDate> existingDates, LocalDate startDate, LocalDate endDate) {
        List<LocalDate> missingDates = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            // 주말 제외 (토요일=6, 일요일=7)
            java.time.DayOfWeek dayOfWeek = current.getDayOfWeek();
            boolean isWeekend = dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY;

            if (!isWeekend && !existingDates.contains(current)) {
                missingDates.add(current);
            }
            current = current.plusDays(1);
        }

        return missingDates;
    }

    /**
     * 빠진 날짜(갭)만 수집
     */
    public CollectionResult collectMissingDates(String symbol) {
        log.info("Collecting MISSING dates for {}", symbol);

        StockDataStatus status = getStockDataStatus(symbol);

        if (!status.isHasData()) {
            return CollectionResult.builder()
                .success(false)
                .message("데이터가 없습니다. 먼저 기본 데이터를 수집해주세요.")
                .build();
        }

        if (!status.isHasGaps()) {
            return CollectionResult.builder()
                .success(true)
                .totalStocks(1)
                .successCount(1)
                .newDataCount(0)
                .message("빠진 날짜가 없습니다.")
                .build();
        }

        List<LocalDate> missingDates = status.getMissingDates();
        log.info("Found {} missing dates for {}: {}", missingDates.size(), symbol, missingDates);

        int newDataCount = 0;
        int failCount = 0;
        List<String> processedDates = new ArrayList<>();
        List<String> failedDates = new ArrayList<>();

        // 빠진 날짜들을 연속 구간으로 그룹화하여 수집
        List<DateRange> ranges = groupConsecutiveDates(missingDates);

        for (DateRange range : ranges) {
            try {
                SingleStockResult result = collectSingleStockData(
                    symbol,
                    range.getStart().atStartOfDay(),
                    range.getEnd().atTime(23, 59, 59)
                );

                if (result.isSuccess()) {
                    newDataCount += result.getNewRecords();
                    processedDates.add(String.format("%s ~ %s: %d건",
                        range.getStart(), range.getEnd(), result.getNewRecords()));
                } else {
                    failCount++;
                    failedDates.add(String.format("%s ~ %s: %s",
                        range.getStart(), range.getEnd(), result.getMessage()));
                }

                // Rate limit
                Thread.sleep(200);

            } catch (Exception e) {
                failCount++;
                failedDates.add(String.format("%s ~ %s: %s",
                    range.getStart(), range.getEnd(), e.getMessage()));
            }
        }

        String message = String.format("갭 수집 완료 - %d개 구간 처리, 신규 데이터: %d건",
            ranges.size(), newDataCount);

        return CollectionResult.builder()
            .success(failCount == 0)
            .totalStocks(1)
            .successCount(failCount == 0 ? 1 : 0)
            .failCount(failCount)
            .newDataCount(newDataCount)
            .processedSymbols(processedDates)
            .failedSymbols(failedDates)
            .message(message)
            .build();
    }

    /**
     * 선택한 종목들의 빠진 날짜(갭)만 수집
     */
    public CollectionResult collectMissingDatesForSymbols(List<String> symbols) {
        log.info("Collecting MISSING dates for {} symbols", symbols.size());

        int totalNewData = 0;
        int successCount = 0;
        int failCount = 0;
        List<String> processedSymbols = new ArrayList<>();
        List<String> failedSymbols = new ArrayList<>();

        for (String symbol : symbols) {
            try {
                CollectionResult result = collectMissingDates(symbol);

                if (result.isSuccess()) {
                    successCount++;
                    totalNewData += result.getNewDataCount();
                    processedSymbols.add(String.format("%s - 신규 %d건", symbol, result.getNewDataCount()));
                } else {
                    failCount++;
                    failedSymbols.add(symbol + "(" + result.getMessage() + ")");
                }

            } catch (Exception e) {
                failCount++;
                failedSymbols.add(symbol + "(" + e.getMessage() + ")");
            }
        }

        String message = String.format("갭 수집 완료 - 성공: %d, 실패: %d, 신규 데이터: %d건",
            successCount, failCount, totalNewData);

        return CollectionResult.builder()
            .success(failCount == 0)
            .totalStocks(symbols.size())
            .successCount(successCount)
            .failCount(failCount)
            .newDataCount(totalNewData)
            .processedSymbols(processedSymbols)
            .failedSymbols(failedSymbols)
            .message(message)
            .build();
    }

    /**
     * 연속 날짜들을 구간으로 그룹화
     */
    private List<DateRange> groupConsecutiveDates(List<LocalDate> dates) {
        if (dates.isEmpty()) {
            return List.of();
        }

        List<LocalDate> sortedDates = new ArrayList<>(dates);
        Collections.sort(sortedDates);

        List<DateRange> ranges = new ArrayList<>();
        LocalDate rangeStart = sortedDates.get(0);
        LocalDate rangeEnd = sortedDates.get(0);

        for (int i = 1; i < sortedDates.size(); i++) {
            LocalDate current = sortedDates.get(i);

            // 주말을 고려하여 연속 여부 판단 (최대 3일 간격까지 연속으로 처리)
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(rangeEnd, current);

            if (daysBetween <= 3) {
                // 연속 구간 확장
                rangeEnd = current;
            } else {
                // 새 구간 시작
                ranges.add(new DateRange(rangeStart, rangeEnd));
                rangeStart = current;
                rangeEnd = current;
            }
        }

        // 마지막 구간 추가
        ranges.add(new DateRange(rangeStart, rangeEnd));

        return ranges;
    }

    /**
     * 날짜 구간 DTO
     */
    @Getter
    @RequiredArgsConstructor
    public static class DateRange {
        private final LocalDate start;
        private final LocalDate end;
    }

    /**
     * KIS API 응답을 CandleHistory 엔티티로 변환
     */
    private CandleHistory convertToCandle(String symbol, KisChartResponse.Output2 output) {
        String dateStr = output.getStckBsopDate();
        LocalDateTime time = LocalDateTime.parse(
            dateStr + "153000",
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        );

        return CandleHistory.builder()
            .symbol(symbol)
            .time(time)
            .open(new BigDecimal(output.getStckOprc()))
            .high(new BigDecimal(output.getStckHgpr()))
            .low(new BigDecimal(output.getStckLwpr()))
            .close(new BigDecimal(output.getStckClpr()))
            .volume(Long.parseLong(output.getAcmlVol()))
            .build();
    }

    /**
     * 보조지표 계산 및 업데이트 (MA20, MA60 등)
     */
    @Transactional
    public void calculateIndicators(String symbol) {
        log.info("Calculating indicators for {}", symbol);

        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusMonths(6);

        List<CandleHistory> candles = candleHistoryRepository
            .findBySymbolAndTimeBetween(symbol, startDate, endDate);

        if (candles.size() < 60) {
            log.warn("Not enough data to calculate MA60 for {}", symbol);
            return;
        }

        candles.sort((a, b) -> a.getTime().compareTo(b.getTime()));

        for (int i = 0; i < candles.size(); i++) {
            CandleHistory candle = candles.get(i);

            if (i >= 19) {
                BigDecimal sum20 = BigDecimal.ZERO;
                for (int j = i - 19; j <= i; j++) {
                    sum20 = sum20.add(candles.get(j).getClose());
                }
                candle.setMa20(sum20.divide(new BigDecimal(20), 4, BigDecimal.ROUND_HALF_UP));
            }

            if (i >= 59) {
                BigDecimal sum60 = BigDecimal.ZERO;
                for (int j = i - 59; j <= i; j++) {
                    sum60 = sum60.add(candles.get(j).getClose());
                }
                candle.setMa60(sum60.divide(new BigDecimal(60), 4, BigDecimal.ROUND_HALF_UP));
            }
        }

        candleHistoryRepository.saveAll(candles);
        log.info("Updated indicators for {} candles of {}", candles.size(), symbol);
    }

    /**
     * 모든 종목의 보조지표 계산
     */
    @Transactional
    public void calculateAllIndicators() {
        log.info("Calculating indicators for all stocks");

        List<StockMaster> stocks = stockMasterRepository.findAll();

        for (StockMaster stock : stocks) {
            try {
                calculateIndicators(stock.getCode());
            } catch (Exception e) {
                log.error("Failed to calculate indicators for {}: {}",
                    stock.getCode(), e.getMessage());
            }
        }

        log.info("Indicator calculation completed for all stocks");
    }

    /**
     * 선택한 종목들만 데이터 수집 (최근 N일)
     */
    public CollectionResult collectSelectedStocks(List<String> symbols, int days) {
        log.info("Starting data collection for {} SELECTED stocks (last {} days)", symbols.size(), days);

        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);

        return collectSymbolsInRange(symbols, startDate, endDate);
    }

    /**
     * 선택한 종목들만 특정 기간 데이터 수집
     */
    public CollectionResult collectSelectedStocksInRange(List<String> symbols, LocalDate start, LocalDate end) {
        log.info("Starting data collection for {} SELECTED stocks from {} to {}", symbols.size(), start, end);

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(23, 59, 59);

        return collectSymbolsInRange(symbols, startDateTime, endDateTime);
    }

    /**
     * 심볼 목록에 대해 특정 기간 데이터 수집 (공통 로직)
     */
    private CollectionResult collectSymbolsInRange(List<String> symbols, LocalDateTime startDate, LocalDateTime endDate) {
        int successCount = 0;
        int failCount = 0;
        int newDataCount = 0;
        List<String> processedSymbols = new ArrayList<>();
        List<String> failedSymbols = new ArrayList<>();

        for (String symbol : symbols) {
            try {
                SingleStockResult result = collectSingleStockData(symbol, startDate, endDate);

                if (result.isSuccess()) {
                    successCount++;
                    newDataCount += result.getNewRecords();
                    processedSymbols.add(String.format("%s - 신규 %d건", symbol, result.getNewRecords()));

                    log.info("✓ {} - 신규: {}건, 스킵: {}건",
                        symbol, result.getNewRecords(), result.getSkippedRecords());
                } else {
                    failCount++;
                    failedSymbols.add(symbol + "(" + result.getMessage() + ")");
                }

                // Rate limit protection (KIS API 제한 고려)
                Thread.sleep(200);

            } catch (Exception e) {
                failCount++;
                failedSymbols.add(symbol + "(" + e.getMessage() + ")");
                log.error("✗ Failed to collect data for {}: {}", symbol, e.getMessage());
            }
        }

        String message = String.format("수집 완료 - 성공: %d, 실패: %d, 신규 데이터: %d건",
            successCount, failCount, newDataCount);

        log.info(message);

        return CollectionResult.builder()
            .success(failCount == 0)
            .totalStocks(symbols.size())
            .successCount(successCount)
            .failCount(failCount)
            .newDataCount(newDataCount)
            .startDate(startDate)
            .endDate(endDate)
            .processedSymbols(processedSymbols)
            .failedSymbols(failedSymbols)
            .message(message)
            .build();
    }

    /**
     * 단일 종목 수집 결과
     */
    @Getter
    @Builder
    public static class SingleStockResult {
        private boolean success;
        private int newRecords;
        private int skippedRecords;
        private String message;
    }

    /**
     * 데이터 수집 결과 DTO
     */
    @Getter
    @Builder
    public static class CollectionResult {
        private boolean success;
        private int totalStocks;
        private int successCount;
        private int failCount;
        private int newDataCount;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private List<String> processedSymbols;
        private List<String> failedSymbols;
        private String message;
    }

    /**
     * 종목 데이터 현황
     */
    @Getter
    @Builder
    public static class StockDataStatus {
        private String symbol;
        private boolean hasData;
        private LocalDate minDate;
        private LocalDate maxDate;
        private int totalDays;
        private Set<LocalDate> existingDates;
        private List<LocalDate> missingDates;
        private boolean hasGaps;
        private int gapCount;
        private int expectedTradingDays;      // 예상 거래일 수 (주말 제외)
        private double completenessRate;       // 완결성 비율 (%)
        private String reliabilityLevel;       // HIGH, MEDIUM, LOW, UNRELIABLE
    }
}
