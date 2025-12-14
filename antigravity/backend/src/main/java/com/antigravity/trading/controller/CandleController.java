package com.antigravity.trading.controller;

import com.antigravity.trading.domain.dto.CandleDto;
import com.antigravity.trading.repository.CandleHistoryRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/candles")
@RequiredArgsConstructor
public class CandleController {

    private final com.antigravity.trading.infrastructure.api.KisApiClient kisApiClient;
    private final CandleHistoryRepository candleHistoryRepository;

    @GetMapping
    public ResponseEntity<List<CandleDto>> getCandles(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "daily") String type,
            @RequestParam(defaultValue = "365") int days) {

        if ("minute".equalsIgnoreCase(type)) {
            return getMinuteCandles(symbol);
        }

        // DB에서 저장된 데이터 조회 (기본: 최근 365일)
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);

        var dbCandles = candleHistoryRepository.findBySymbolAndTimeBetween(symbol, startDate, endDate);

        if (!dbCandles.isEmpty()) {
            // DB에 데이터가 있으면 DB 데이터 사용
            List<CandleDto> candles = dbCandles.stream()
                    .map(candle -> CandleDto.builder()
                            .time(candle.getTime().toLocalDate().toString())
                            .open(candle.getOpen())
                            .high(candle.getHigh())
                            .low(candle.getLow())
                            .close(candle.getClose())
                            .volume(java.math.BigDecimal.valueOf(candle.getVolume()))
                            .build())
                    .sorted((a, b) -> a.getTime().compareTo(b.getTime()))
                    .toList();

            return ResponseEntity.ok(candles);
        }

        // DB에 데이터가 없으면 KIS API 호출 (fallback)
        var chartResponse = kisApiClient.getDailyChart(symbol);

        if (chartResponse == null || chartResponse.getOutput2() == null) {
            return ResponseEntity.ok(List.of());
        }

        // Convert to DTO (Simplified for Frontend)
        List<CandleDto> candles = chartResponse.getOutput2().stream()
                .map(this::toDto)
                .sorted((a, b) -> a.getTime().compareTo(b.getTime()))
                .toList();

        return ResponseEntity.ok(candles);
    }

    private ResponseEntity<List<CandleDto>> getMinuteCandles(String symbol) {
        var response = kisApiClient.getMinuteChart(symbol);
        if (response == null || response.getOutput2() == null) {
            return ResponseEntity.ok(List.of());
        }

        List<CandleDto> candles = response.getOutput2().stream()
                .map(this::toMinuteDto)
                .sorted((a, b) -> a.getTime().compareTo(b.getTime()))
                .toList();
        return ResponseEntity.ok(candles);
    }

    private CandleDto toDto(com.antigravity.trading.infrastructure.api.dto.KisChartResponse.Output2 output) {
        // output.stckBsopDate -> "20241212"
        String dateStr = output.getStckBsopDate();
        String formattedDate = dateStr.substring(0, 4) + "-" + dateStr.substring(4, 6) + "-" + dateStr.substring(6, 8);

        return CandleDto.builder()
                .time(formattedDate)
                .open(new java.math.BigDecimal(output.getStckOprc()))
                .high(new java.math.BigDecimal(output.getStckHgpr()))
                .low(new java.math.BigDecimal(output.getStckLwpr()))
                .close(new java.math.BigDecimal(output.getStckClpr()))
                .volume(new java.math.BigDecimal(output.getAcmlVol()))
                .build();
    }

    private CandleDto toMinuteDto(
            com.antigravity.trading.infrastructure.api.dto.KisMinuteChartResponse.Output2 output) {
        // output.stckBsopTime -> "123000" (HHMMSS) or stckCntgHour
        // toMinuteDto logic in Step 778 used toDtoMinute from diff.
        // Step 778 implementation used output.getStckCntgHour()

        String timeStr = output.getStckBsopTime(); // getStckBsopTime for Minute Chart

        // Format for frontend if needed? Frontend uses Lightweight Charts which parses
        // yyyy-MM-dd or timestamp.
        // If "123000", convert to "2024-12-13 12:30:00" or just send raw?
        // App.tsx expects time string.
        // Let's format it.
        String today = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd").format(java.time.LocalDate.now());
        String formattedTime = today + " " + timeStr.substring(0, 2) + ":" + timeStr.substring(2, 4);

        return CandleDto.builder()
                .time(formattedTime) // "2024-12-13 12:30"
                .open(new java.math.BigDecimal(output.getStckOprc()))
                .high(new java.math.BigDecimal(output.getStckHgpr()))
                .low(new java.math.BigDecimal(output.getStckLwpr()))
                .close(new java.math.BigDecimal(output.getStckPrpr()))
                .volume(new java.math.BigDecimal(output.getAcmlVol()))
                .build();
    }

    /**
     * 특정 종목의 특정 날짜 데이터 삭제 (잘못된 데이터 정리용)
     * DELETE /api/candles?symbol=314130&date=2025-12-14
     */
    @DeleteMapping
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Map<String, Object>> deleteCandle(
            @RequestParam String symbol,
            @RequestParam String date) {

        Map<String, Object> response = new HashMap<>();

        try {
            // date: "2025-12-14" 형식
            java.time.LocalDate localDate = java.time.LocalDate.parse(date);
            LocalDateTime startOfDay = localDate.atStartOfDay();
            LocalDateTime endOfDay = localDate.atTime(23, 59, 59);

            // 삭제 전 데이터 확인
            var candles = candleHistoryRepository.findBySymbolAndTimeBetween(symbol, startOfDay, endOfDay);

            if (candles.isEmpty()) {
                response.put("success", false);
                response.put("message", "No data found for " + symbol + " on " + date);
                return ResponseEntity.ok(response);
            }

            // 삭제
            candleHistoryRepository.deleteBySymbolAndTimeBetween(symbol, startOfDay, endOfDay);

            response.put("success", true);
            response.put("message", "Deleted " + candles.size() + " candle(s) for " + symbol + " on " + date);
            response.put("deletedCount", candles.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 특정 종목의 데이터 보유 기간 조회
     * GET /api/candles/data-range?symbol=005930
     */
    @GetMapping("/data-range")
    public ResponseEntity<Map<String, Object>> getDataRange(@RequestParam String symbol) {
        Map<String, Object> response = new HashMap<>();

        // DB에서 해당 종목의 최소/최대 날짜 조회
        LocalDateTime minDate = candleHistoryRepository.findMinTimeBySymbol(symbol);
        LocalDateTime maxDate = candleHistoryRepository.findMaxTimeBySymbol(symbol);

        if (minDate == null || maxDate == null) {
            response.put("hasData", false);
            response.put("message", "No data available for symbol: " + symbol);
            return ResponseEntity.ok(response);
        }

        response.put("hasData", true);
        response.put("symbol", symbol);
        response.put("minDate", minDate.toLocalDate().toString());
        response.put("maxDate", maxDate.toLocalDate().toString());
        response.put("totalDays", java.time.temporal.ChronoUnit.DAYS.between(minDate.toLocalDate(), maxDate.toLocalDate()) + 1);

        return ResponseEntity.ok(response);
    }
}
