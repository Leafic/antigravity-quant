package com.antigravity.trading.controller;

import com.antigravity.trading.domain.entity.TradeLog;
import com.antigravity.trading.repository.TradeLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeLogController {

    private final TradeLogRepository tradeLogRepository;

    @GetMapping
    public ResponseEntity<List<TradeLog>> getTradeLogs() {
        return ResponseEntity.ok(tradeLogRepository.findAllByOrderByTimestampDesc());
    }
}
