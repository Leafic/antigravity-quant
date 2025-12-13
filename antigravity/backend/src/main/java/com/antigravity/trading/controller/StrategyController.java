package com.antigravity.trading.controller;

import com.antigravity.trading.engine.StrategyRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/strategies")
@RequiredArgsConstructor
public class StrategyController {

    private final StrategyRegistry strategyRegistry;

    @GetMapping
    public List<Map<String, String>> getStrategies() {
        return strategyRegistry.getStrategyMetadata();
    }
}
