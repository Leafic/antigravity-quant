package com.antigravity.trading.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/candles")
@RequiredArgsConstructor
public class CandleController {

    private final com.antigravity.trading.infrastructure.api.KisApiClient kisApiClient;

    @GetMapping
    public ResponseEntity<List<CandleDto>> getCandles(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "daily") String type) {

        if ("minute".equalsIgnoreCase(type)) {
            return getMinuteCandles(symbol);
        }

        // Default: Daily
        var chartResponse = kisApiClient.getDailyChart(symbol);

        if (chartResponse == null || chartResponse.getOutput2() == null) {
            return ResponseEntity.ok(List.of());
        }

        // Convert to DTO (Simplified for Frontend)
        List<CandleDto> candles = chartResponse.getOutput2().stream()
                .map(this::toDto)
                .sorted((a, b) -> a.getTime().compareTo(b.getTime())) // KIS default might be descending
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

        return new CandleDto(
                formattedDate, // treating as string for simple frontend handling
                new java.math.BigDecimal(output.getStckOprc()),
                new java.math.BigDecimal(output.getStckHgpr()),
                new java.math.BigDecimal(output.getStckLwpr()),
                new java.math.BigDecimal(output.getStckClpr()));
    }

    private CandleDto toMinuteDto(
            com.antigravity.trading.infrastructure.api.dto.KisMinuteChartResponse.Output2 output) {
        // output.stckBsopTime -> "123000" (HHMMSS)
        // For minute chart, we might want "YYYY-MM-DD HH:mm:ss" or just time?
        // Lightweight charts prefers full timestamp or time.
        // We'll append Today's date because FHKST03010200 is "Intra-day".
        String timeStr = output.getStckBsopTime();
        String today = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd").format(java.time.LocalDate.now());
        String formattedTime = today + " " + timeStr.substring(0, 2) + ":" + timeStr.substring(2, 4); // + ":" +
                                                                                                      // timeStr.substring(4,
                                                                                                      // 6);

        return new CandleDto(
                formattedTime,
                new java.math.BigDecimal(output.getStckOprc()),
                new java.math.BigDecimal(output.getStckHgpr()),
                new java.math.BigDecimal(output.getStckLwpr()),
                new java.math.BigDecimal(output.getStckPrpr()) // Minute chart uses 'prpr' as close
        );
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class CandleDto {
        private String time;
        private java.math.BigDecimal open;
        private java.math.BigDecimal high;
        private java.math.BigDecimal low;
        private java.math.BigDecimal close;
    }
}
