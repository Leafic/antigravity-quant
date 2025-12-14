package com.antigravity.trading.controller;

import com.antigravity.trading.infrastructure.api.dto.KisConditionSearchListResponse;
import com.antigravity.trading.infrastructure.api.dto.KisConditionSearchResultResponse;
import com.antigravity.trading.service.BacktestService;
import com.antigravity.trading.service.ConditionSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 조건검색 API 컨트롤러
 * 한국투자증권 조건검색과 백테스트 통합
 */
@Slf4j
@RestController
@RequestMapping("/api/condition-search")
@RequiredArgsConstructor
public class ConditionSearchController {

    private final ConditionSearchService conditionSearchService;
    private final BacktestService backtestService;

    /**
     * 조건검색 목록 조회
     * GET /api/condition-search/list?userId=abc
     */
    @GetMapping("/list")
    public ResponseEntity<List<KisConditionSearchListResponse.ConditionItem>> getConditionList(
            @RequestParam(required = false) String userId) {

        log.info("Fetching condition search list - userId: {}", userId);

        List<KisConditionSearchListResponse.ConditionItem> conditions =
                conditionSearchService.getConditionList(userId);

        return ResponseEntity.ok(conditions);
    }

    /**
     * 조건검색 종목 조회
     * GET /api/condition-search/stocks?userId=abc&seq=0
     */
    @GetMapping("/stocks")
    public ResponseEntity<List<KisConditionSearchResultResponse.StockItem>> searchStocks(
            @RequestParam(required = false) String userId,
            @RequestParam String seq) {

        log.info("Searching stocks - userId: {}, seq: {}", userId, seq);

        List<KisConditionSearchResultResponse.StockItem> stocks =
                conditionSearchService.searchStocks(userId, seq);

        return ResponseEntity.ok(stocks);
    }

    /**
     * 조건검색 + 백테스트 실행
     * POST /api/condition-search/backtest
     *
     * Body:
     * {
     *   "userId": "abc",
     *   "seq": "0",
     *   "startDate": "2023-01-01T00:00:00",
     *   "endDate": "2023-12-31T23:59:59",
     *   "strategyId": "S1"
     * }
     */
    @PostMapping("/backtest")
    public ResponseEntity<Map<String, Object>> runBacktestOnCondition(
            @RequestBody ConditionBacktestRequest request) {

        log.info("Running backtest on condition search - userId: {}, seq: {}, strategy: {}",
                request.getUserId(), request.getSeq(), request.getStrategyId());

        try {
            // 1. 조건검색으로 종목 코드 가져오기
            List<String> stockCodes = conditionSearchService.getStockCodes(
                    request.getUserId(),
                    request.getSeq()
            );

            if (stockCodes.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "No stocks found for the given condition");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            log.info("Found {} stocks, running backtest on each", stockCodes.size());

            // 2. 각 종목별로 백테스트 실행
            List<Map<String, Object>> results = stockCodes.stream()
                    .map(symbol -> runBacktestForSymbol(
                            symbol,
                            request.getStartDate(),
                            request.getEndDate(),
                            request.getStrategyId()
                    ))
                    .toList();

            // 3. 결과 집계
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalStocks", stockCodes.size());
            response.put("results", results);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Backtest failed: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Backtest failed: " + e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 개별 종목 백테스트 실행 (헬퍼 메서드)
     */
    private Map<String, Object> runBacktestForSymbol(
            String symbol,
            LocalDateTime start,
            LocalDateTime end,
            String strategyId) {

        Map<String, Object> result = new HashMap<>();
        result.put("symbol", symbol);

        try {
            BacktestService.BacktestResult backtestResult =
                    backtestService.runBacktest(symbol, start, end, strategyId, null);

            result.put("success", true);
            result.put("finalBalance", backtestResult.getFinalBalance());
            result.put("returnPercent", backtestResult.getTotalReturnPercent());
            result.put("totalTrades", backtestResult.getTotalTrades());

        } catch (Exception e) {
            log.error("Backtest failed for {}: {}", symbol, e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * 요청 DTO
     */
    public static class ConditionBacktestRequest {
        private String userId;
        private String seq;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime startDate;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime endDate;
        private String strategyId = "S1";

        // Getters and Setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getSeq() { return seq; }
        public void setSeq(String seq) { this.seq = seq; }

        public LocalDateTime getStartDate() { return startDate; }
        public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

        public LocalDateTime getEndDate() { return endDate; }
        public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

        public String getStrategyId() { return strategyId; }
        public void setStrategyId(String strategyId) { this.strategyId = strategyId; }
    }
}
