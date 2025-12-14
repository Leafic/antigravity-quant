package com.antigravity.trading.controller;

import com.antigravity.trading.domain.entity.SchedulerHistory;
import com.antigravity.trading.repository.SchedulerHistoryRepository;
import com.antigravity.trading.scheduler.DataCollectionScheduler;
import com.antigravity.trading.service.DataPipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 데이터 파이프라인 수동 제어용 API
 * 테스트 및 디버깅, 백필용
 */
@Slf4j
@RestController
@RequestMapping("/api/data-pipeline")
@RequiredArgsConstructor
public class DataPipelineController {

    private final DataPipelineService dataPipelineService;
    private final SchedulerHistoryRepository schedulerHistoryRepository;

    @Autowired(required = false)
    private DataCollectionScheduler scheduler;

    /**
     * 데이터 수동 수집 (기본: 스케줄된 종목만, all=true시 전체 종목)
     * POST /api/data-pipeline/collect?days=100&all=false
     */
    @PostMapping("/collect")
    public ResponseEntity<Map<String, Object>> collectAllData(
            @RequestParam(defaultValue = "100") int days,
            @RequestParam(defaultValue = "false") boolean all) {

        String target = all ? "ALL stocks" : "SCHEDULED stocks only";
        log.info("Manual data collection triggered via API for {} ({})", days, target);

        // 히스토리 기록 시작
        SchedulerHistory history = new SchedulerHistory();
        history.setJobName(all ? "ALL_STOCKS_COLLECTION_MANUAL" : "SCHEDULED_STOCKS_COLLECTION_MANUAL");
        history.setStartTime(LocalDateTime.now());
        history.setStatus("RUNNING");
        history = schedulerHistoryRepository.save(history);

        DataPipelineService.CollectionResult result;

        try {
            // 스케줄러가 없으면 서비스 직접 호출
            log.warn("Scheduler not available, calling service directly");

            if (all) {
                // 전체 종목 수집
                result = dataPipelineService.collectAllStockData(days);
            } else {
                // 스케줄된 종목만 수집 (기본)
                result = dataPipelineService.collectScheduledStocks(days);
            }

            // 성공 기록
            history.setEndTime(LocalDateTime.now());
            history.setStatus("SUCCESS");

            String message = String.format(
                "Manual data collection completed: %d/%d stocks (Period: %s ~ %s)",
                result.getSuccessCount(), result.getTotalStocks(),
                result.getStartDate().toLocalDate(), result.getEndDate().toLocalDate()
            );

            if (result.getFailedSymbols() != null && !result.getFailedSymbols().isEmpty()) {
                message += "\nFailed: " + String.join(", ", result.getFailedSymbols());
            }

            if (result.getProcessedSymbols() != null && !result.getProcessedSymbols().isEmpty()) {
                // 처음 10개만 히스토리에 기록
                int maxToShow = Math.min(10, result.getProcessedSymbols().size());
                message += "\nProcessed: " + String.join(", ", result.getProcessedSymbols().subList(0, maxToShow));
                if (result.getProcessedSymbols().size() > 10) {
                    message += " ... and " + (result.getProcessedSymbols().size() - 10) + " more";
                }
            }

            history.setMessage(message);
            schedulerHistoryRepository.save(history);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", all ? "Data collection completed for all stocks" : "Data collection completed for scheduled stocks");
            response.put("totalStocks", result.getTotalStocks());
            response.put("successCount", result.getSuccessCount());
            response.put("failCount", result.getFailCount());
            response.put("startDate", result.getStartDate());
            response.put("endDate", result.getEndDate());
            response.put("days", days);
            response.put("collectionType", all ? "ALL" : "SCHEDULED");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Data collection failed: {}", e.getMessage(), e);

            // 실패 기록
            history.setEndTime(LocalDateTime.now());
            history.setStatus("FAILED");
            history.setMessage("Data collection failed: " + e.getMessage());
            schedulerHistoryRepository.save(history);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Data collection failed: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 특정 종목 데이터 수동 수집
     * POST /api/data-pipeline/collect/{symbol}?days=100
     */
    @PostMapping("/collect/{symbol}")
    public ResponseEntity<Map<String, Object>> collectStockData(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int days) {

        log.info("Manual data collection triggered for {} (last {} days)", symbol, days);

        // 히스토리 기록 시작
        SchedulerHistory history = new SchedulerHistory();
        history.setJobName("SINGLE_STOCK_COLLECTION_MANUAL");
        history.setStartTime(LocalDateTime.now());
        history.setStatus("RUNNING");
        history = schedulerHistoryRepository.save(history);

        try {
            LocalDateTime startDate = LocalDateTime.now().minusDays(days);
            LocalDateTime endDate = LocalDateTime.now();

            if (scheduler != null) {
                scheduler.triggerStockCollection(symbol, days);
            } else {
                // 스케줄러가 없으면 서비스 직접 호출
                log.warn("Scheduler not available for {}, falling back to direct service call", symbol);
                dataPipelineService.collectStockData(symbol, days);
            }

            // 성공 기록
            history.setEndTime(LocalDateTime.now());
            history.setStatus("SUCCESS");
            history.setMessage(String.format(
                "Manual collection completed for %s (Period: %s ~ %s, %d days)",
                symbol, startDate.toLocalDate(), endDate.toLocalDate(), days
            ));
            schedulerHistoryRepository.save(history);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Data collection completed");
            response.put("symbol", symbol);
            response.put("startDate", startDate);
            response.put("endDate", endDate);
            response.put("days", days);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Data collection failed for {}: {}", symbol, e.getMessage(), e);

            // 실패 기록
            history.setEndTime(LocalDateTime.now());
            history.setStatus("FAILED");
            history.setMessage(String.format("Collection failed for %s: %s", symbol, e.getMessage()));
            schedulerHistoryRepository.save(history);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Data collection failed: " + e.getMessage());
            response.put("symbol", symbol);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 특정 종목의 특정 기간 데이터 수집 (백필용)
     * POST /api/data-pipeline/backfill/{symbol}?start=2023-01-01&end=2023-12-31
     */
    @PostMapping("/backfill/{symbol}")
    public ResponseEntity<Map<String, Object>> backfillData(
            @PathVariable String symbol,
            @RequestParam String start,
            @RequestParam String end) {

        log.info("Backfill triggered for {} from {} to {}", symbol, start, end);

        try {
            LocalDateTime startDate = LocalDateTime.parse(start + "T00:00:00");
            LocalDateTime endDate = LocalDateTime.parse(end + "T23:59:59");

            dataPipelineService.collectStockDataRange(symbol, startDate, endDate);
            dataPipelineService.calculateIndicators(symbol);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Backfill completed");
            response.put("symbol", symbol);
            response.put("start", start);
            response.put("end", end);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Backfill failed for {}: {}", symbol, e.getMessage(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Backfill failed: " + e.getMessage());
            response.put("symbol", symbol);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 보조지표 재계산
     * POST /api/data-pipeline/calculate-indicators
     */
    @PostMapping("/calculate-indicators")
    public ResponseEntity<Map<String, Object>> calculateIndicators() {
        log.info("Manual indicator calculation triggered via API");

        try {
            dataPipelineService.calculateAllIndicators();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Indicators calculated for all stocks");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Indicator calculation failed: {}", e.getMessage(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Indicator calculation failed: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 날짜 범위 지정 데이터 수집 (스케줄된 종목 대상)
     * POST /api/data-pipeline/collect-range?start=2024-01-01&end=2024-12-31
     */
    @PostMapping("/collect-range")
    public ResponseEntity<Map<String, Object>> collectDataInRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        log.info("Manual data collection triggered for date range: {} to {}", start, end);

        // 히스토리 기록 시작
        SchedulerHistory history = new SchedulerHistory();
        history.setJobName("SCHEDULED_STOCKS_RANGE_COLLECTION_MANUAL");
        history.setStartTime(LocalDateTime.now());
        history.setStatus("RUNNING");
        history = schedulerHistoryRepository.save(history);

        try {
            DataPipelineService.CollectionResult result = dataPipelineService.collectScheduledStocksInRange(start, end);

            // 성공 기록
            history.setEndTime(LocalDateTime.now());
            history.setStatus(result.isSuccess() ? "SUCCESS" : "PARTIAL");
            history.setSuccessItems(result.getSuccessCount());
            history.setFailedItems(result.getFailCount());
            history.setTotalItems(result.getTotalStocks());
            history.setMessage(result.getMessage());

            if (result.getFailedSymbols() != null && !result.getFailedSymbols().isEmpty()) {
                history.setErrorDetails(String.join("\n", result.getFailedSymbols()));
            }
            schedulerHistoryRepository.save(history);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("totalStocks", result.getTotalStocks());
            response.put("successCount", result.getSuccessCount());
            response.put("failCount", result.getFailCount());
            response.put("newDataCount", result.getNewDataCount());
            response.put("startDate", start);
            response.put("endDate", end);
            response.put("processedSymbols", result.getProcessedSymbols());
            response.put("failedSymbols", result.getFailedSymbols());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Data collection failed: {}", e.getMessage(), e);

            history.setEndTime(LocalDateTime.now());
            history.setStatus("FAILED");
            history.setMessage("Data collection failed: " + e.getMessage());
            schedulerHistoryRepository.save(history);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Data collection failed: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 종목별 데이터 현황 조회
     * GET /api/data-pipeline/status/{symbol}
     */
    @GetMapping("/status/{symbol}")
    public ResponseEntity<DataPipelineService.StockDataStatus> getStockDataStatus(@PathVariable String symbol) {
        DataPipelineService.StockDataStatus status = dataPipelineService.getStockDataStatus(symbol);
        return ResponseEntity.ok(status);
    }

    /**
     * 선택한 종목들만 데이터 수집 (최근 N일)
     * POST /api/data-pipeline/collect-selected?days=100
     * Body: ["005930", "000660", ...]
     */
    @PostMapping("/collect-selected")
    public ResponseEntity<Map<String, Object>> collectSelectedStocks(
            @RequestBody List<String> symbols,
            @RequestParam(defaultValue = "100") int days) {

        log.info("Manual data collection triggered for {} selected stocks (last {} days)", symbols.size(), days);

        if (symbols.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "선택된 종목이 없습니다.");
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.badRequest().body(response);
        }

        // 히스토리 기록 시작
        SchedulerHistory history = new SchedulerHistory();
        history.setJobName("SELECTED_STOCKS_COLLECTION_MANUAL");
        history.setStartTime(LocalDateTime.now());
        history.setStatus("RUNNING");
        history.setTotalItems(symbols.size());
        history = schedulerHistoryRepository.save(history);

        try {
            DataPipelineService.CollectionResult result = dataPipelineService.collectSelectedStocks(symbols, days);

            // 성공 기록
            history.setEndTime(LocalDateTime.now());
            history.setStatus(result.isSuccess() ? "SUCCESS" : "PARTIAL");
            history.setSuccessItems(result.getSuccessCount());
            history.setFailedItems(result.getFailCount());
            history.setMessage(result.getMessage());

            if (result.getFailedSymbols() != null && !result.getFailedSymbols().isEmpty()) {
                history.setErrorDetails(String.join("\n", result.getFailedSymbols()));
            }
            schedulerHistoryRepository.save(history);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("totalStocks", result.getTotalStocks());
            response.put("successCount", result.getSuccessCount());
            response.put("failCount", result.getFailCount());
            response.put("newDataCount", result.getNewDataCount());
            response.put("startDate", result.getStartDate());
            response.put("endDate", result.getEndDate());
            response.put("days", days);
            response.put("processedSymbols", result.getProcessedSymbols());
            response.put("failedSymbols", result.getFailedSymbols());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Data collection failed: {}", e.getMessage(), e);

            history.setEndTime(LocalDateTime.now());
            history.setStatus("FAILED");
            history.setMessage("Data collection failed: " + e.getMessage());
            schedulerHistoryRepository.save(history);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Data collection failed: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 선택한 종목들만 날짜 범위 지정 데이터 수집
     * POST /api/data-pipeline/collect-selected-range?start=2024-01-01&end=2024-12-31
     * Body: ["005930", "000660", ...]
     */
    @PostMapping("/collect-selected-range")
    public ResponseEntity<Map<String, Object>> collectSelectedStocksInRange(
            @RequestBody List<String> symbols,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        log.info("Manual data collection triggered for {} selected stocks from {} to {}", symbols.size(), start, end);

        if (symbols.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "선택된 종목이 없습니다.");
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.badRequest().body(response);
        }

        // 히스토리 기록 시작
        SchedulerHistory history = new SchedulerHistory();
        history.setJobName("SELECTED_STOCKS_RANGE_COLLECTION_MANUAL");
        history.setStartTime(LocalDateTime.now());
        history.setStatus("RUNNING");
        history.setTotalItems(symbols.size());
        history = schedulerHistoryRepository.save(history);

        try {
            DataPipelineService.CollectionResult result = dataPipelineService.collectSelectedStocksInRange(symbols, start, end);

            // 성공 기록
            history.setEndTime(LocalDateTime.now());
            history.setStatus(result.isSuccess() ? "SUCCESS" : "PARTIAL");
            history.setSuccessItems(result.getSuccessCount());
            history.setFailedItems(result.getFailCount());
            history.setMessage(result.getMessage());

            if (result.getFailedSymbols() != null && !result.getFailedSymbols().isEmpty()) {
                history.setErrorDetails(String.join("\n", result.getFailedSymbols()));
            }
            schedulerHistoryRepository.save(history);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("totalStocks", result.getTotalStocks());
            response.put("successCount", result.getSuccessCount());
            response.put("failCount", result.getFailCount());
            response.put("newDataCount", result.getNewDataCount());
            response.put("startDate", start);
            response.put("endDate", end);
            response.put("processedSymbols", result.getProcessedSymbols());
            response.put("failedSymbols", result.getFailedSymbols());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Data collection failed: {}", e.getMessage(), e);

            history.setEndTime(LocalDateTime.now());
            history.setStatus("FAILED");
            history.setMessage("Data collection failed: " + e.getMessage());
            schedulerHistoryRepository.save(history);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Data collection failed: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(500).body(response);
        }
    }
}
