package com.antigravity.trading.controller;

import com.antigravity.trading.domain.entity.CandleHistory;
import com.antigravity.trading.repository.CandleHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/candles")
@RequiredArgsConstructor
public class CandleController {

    private final com.antigravity.trading.infrastructure.api.KisApiClient kisApiClient;

    @GetMapping
    public ResponseEntity<List<CandleDto>> getCandles(@RequestParam String symbol) {
        // Fetch Real Daily Chart from KIS API
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
