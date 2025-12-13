package com.antigravity.trading.controller;

import com.antigravity.trading.domain.dto.CandleDto;
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
}
