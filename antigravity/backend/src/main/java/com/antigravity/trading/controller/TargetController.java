package com.antigravity.trading.controller;

import com.antigravity.trading.domain.entity.TargetStock;
import com.antigravity.trading.repository.TargetStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/targets")
@RequiredArgsConstructor
public class TargetController {

    private final TargetStockRepository targetStockRepository;

    @GetMapping
    public ResponseEntity<List<TargetStock>> getActiveTargets() {
        // Return only active targets for the dashboard
        return ResponseEntity.ok(targetStockRepository.findByIsActiveTrue());
    }
}
