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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
    @Transactional
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

        int successCount = 0;
        int failCount = 0;
        List<String> processedSymbols = new ArrayList<>();
        List<String> failedSymbols = new ArrayList<>();

        for (ScheduledStock scheduledStock : scheduledStocks) {
            try {
                collectStockData(scheduledStock.getSymbol(), days);
                successCount++;
                processedSymbols.add(scheduledStock.getSymbol() + "(" + scheduledStock.getName() + ")");
                log.info("✓ Collected data for {} ({}) - Progress: {}/{}",
                    scheduledStock.getName(), scheduledStock.getSymbol(),
                    successCount + failCount, scheduledStocks.size());

                // Rate limit protection (KIS API 제한 고려)
                Thread.sleep(200); // 200ms 대기

            } catch (Exception e) {
                failCount++;
                failedSymbols.add(scheduledStock.getSymbol() + "(" + e.getMessage() + ")");
                log.error("✗ Failed to collect data for {} ({}): {}",
                    scheduledStock.getName(), scheduledStock.getSymbol(), e.getMessage());
            }
        }

        log.info("Scheduled stock data collection completed. Success: {}, Failed: {}, Total: {}",
            successCount, failCount, scheduledStocks.size());

        return CollectionResult.builder()
            .success(failCount == 0)
            .totalStocks(scheduledStocks.size())
            .successCount(successCount)
            .failCount(failCount)
            .startDate(startDate)
            .endDate(endDate)
            .processedSymbols(processedSymbols)
            .failedSymbols(failedSymbols)
            .build();
    }

    /**
     * 모든 종목의 일봉 데이터 수집 (관리자 전용)
     * @param days 수집할 일수 (기본: 100일)
     */
    @Transactional
    public CollectionResult collectAllStockData(int days) {
        log.info("Starting data collection for ALL stocks (last {} days)", days);

        List<StockMaster> stocks = stockMasterRepository.findAll();
        log.info("Found {} stocks to process", stocks.size());

        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);

        int successCount = 0;
        int failCount = 0;
        List<String> processedSymbols = new ArrayList<>();
        List<String> failedSymbols = new ArrayList<>();

        for (StockMaster stock : stocks) {
            try {
                collectStockData(stock.getCode(), days);
                successCount++;
                processedSymbols.add(stock.getCode() + "(" + stock.getName() + ")");
                log.info("✓ Collected data for {} ({}) - Progress: {}/{}",
                    stock.getName(), stock.getCode(), successCount + failCount, stocks.size());

                // Rate limit protection (KIS API 제한 고려)
                Thread.sleep(200); // 200ms 대기

            } catch (Exception e) {
                failCount++;
                failedSymbols.add(stock.getCode() + "(" + e.getMessage() + ")");
                log.error("✗ Failed to collect data for {} ({}): {}",
                    stock.getName(), stock.getCode(), e.getMessage());
            }
        }

        log.info("Data collection completed. Success: {}, Failed: {}, Total: {}",
            successCount, failCount, stocks.size());

        return CollectionResult.builder()
            .success(failCount == 0)
            .totalStocks(stocks.size())
            .successCount(successCount)
            .failCount(failCount)
            .startDate(startDate)
            .endDate(endDate)
            .processedSymbols(processedSymbols)
            .failedSymbols(failedSymbols)
            .build();
    }

    /**
     * 특정 종목의 일봉 데이터 수집
     * @param symbol 종목코드
     * @param days 수집할 일수
     */
    @Transactional
    public void collectStockData(String symbol, int days) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);

        log.debug("Collecting data for {} from {} to {}", symbol, startDate, endDate);

        // KIS API에서 데이터 가져오기
        KisChartResponse response = kisApiClient.getDailyChart(symbol, startDate, endDate);

        if (response == null || response.getOutput2() == null || response.getOutput2().isEmpty()) {
            log.warn("No data received from KIS API for {}", symbol);
            return;
        }

        List<CandleHistory> candles = new ArrayList<>();

        for (KisChartResponse.Output2 output : response.getOutput2()) {
            CandleHistory candle = convertToCandle(symbol, output);
            candles.add(candle);
        }

        // 기존 데이터 삭제 후 새로 저장 (Upsert 대신 Replace 전략)
        candleHistoryRepository.deleteAll(
            candleHistoryRepository.findBySymbolAndTimeBetween(symbol, startDate, endDate)
        );

        // 일괄 저장
        candleHistoryRepository.saveAll(candles);

        log.info("Saved {} candles for {}", candles.size(), symbol);
    }

    /**
     * 특정 기간의 데이터만 수집 (백필용)
     */
    @Transactional
    public void collectStockDataRange(String symbol, LocalDateTime start, LocalDateTime end) {
        log.info("Collecting data for {} from {} to {}", symbol, start, end);

        KisChartResponse response = kisApiClient.getDailyChart(symbol, start, end);

        if (response == null || response.getOutput2() == null || response.getOutput2().isEmpty()) {
            log.warn("No data received from KIS API for {}", symbol);
            return;
        }

        List<CandleHistory> candles = new ArrayList<>();

        for (KisChartResponse.Output2 output : response.getOutput2()) {
            CandleHistory candle = convertToCandle(symbol, output);
            candles.add(candle);
        }

        // 기존 데이터와 중복 방지
        candleHistoryRepository.deleteAll(
            candleHistoryRepository.findBySymbolAndTimeBetween(symbol, start, end)
        );

        candleHistoryRepository.saveAll(candles);

        log.info("Saved {} candles for {} (range: {} to {})",
            candles.size(), symbol, start, end);
    }

    /**
     * KIS API 응답을 CandleHistory 엔티티로 변환
     */
    private CandleHistory convertToCandle(String symbol, KisChartResponse.Output2 output) {
        // 날짜 파싱 (yyyyMMdd → LocalDateTime)
        String dateStr = output.getStckBsopDate(); // "20231201"
        LocalDateTime time = LocalDateTime.parse(
            dateStr + "153000", // 15:30:00 (장 마감 시간)
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
            // ma20, ma60은 나중에 계산하여 업데이트
            .build();
    }

    /**
     * 보조지표 계산 및 업데이트 (MA20, MA60 등)
     * 데이터 수집 후 별도로 실행
     */
    @Transactional
    public void calculateIndicators(String symbol) {
        log.info("Calculating indicators for {}", symbol);

        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusMonths(6); // 6개월치

        List<CandleHistory> candles = candleHistoryRepository
            .findBySymbolAndTimeBetween(symbol, startDate, endDate);

        if (candles.size() < 60) {
            log.warn("Not enough data to calculate MA60 for {}", symbol);
            return;
        }

        // 시간순 정렬
        candles.sort((a, b) -> a.getTime().compareTo(b.getTime()));

        for (int i = 0; i < candles.size(); i++) {
            CandleHistory candle = candles.get(i);

            // MA20 계산
            if (i >= 19) {
                BigDecimal sum20 = BigDecimal.ZERO;
                for (int j = i - 19; j <= i; j++) {
                    sum20 = sum20.add(candles.get(j).getClose());
                }
                candle.setMa20(sum20.divide(new BigDecimal(20), 4, BigDecimal.ROUND_HALF_UP));
            }

            // MA60 계산
            if (i >= 59) {
                BigDecimal sum60 = BigDecimal.ZERO;
                for (int j = i - 59; j <= i; j++) {
                    sum60 = sum60.add(candles.get(j).getClose());
                }
                candle.setMa60(sum60.divide(new BigDecimal(60), 4, BigDecimal.ROUND_HALF_UP));
            }
        }

        // 일괄 업데이트
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
     * 데이터 수집 결과 DTO
     */
    @Getter
    @Builder
    public static class CollectionResult {
        private boolean success;
        private int totalStocks;
        private int successCount;
        private int failCount;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private List<String> processedSymbols;
        private List<String> failedSymbols;
    }
}
