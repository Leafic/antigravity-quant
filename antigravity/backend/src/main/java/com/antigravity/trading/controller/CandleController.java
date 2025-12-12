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

    private final CandleHistoryRepository candleHistoryRepository;

    @GetMapping
    public ResponseEntity<List<CandleHistory>> getCandles(
            @RequestParam String symbol,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        // Default range: Last 24 hours if not specified
        LocalDateTime effectiveEnd = (end != null) ? end : LocalDateTime.now();
        LocalDateTime effectiveStart = (start != null) ? start : effectiveEnd.minusHours(24);

        return ResponseEntity
                .ok(candleHistoryRepository.findBySymbolAndTimeBetween(symbol, effectiveStart, effectiveEnd));
    }
}
