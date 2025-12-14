package com.antigravity.trading.engine;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class StrategyRegistry {

    private final Map<String, com.antigravity.trading.strategy.v2.TradingStrategy> strategies = new HashMap<>();

    public StrategyRegistry(List<com.antigravity.trading.strategy.v2.TradingStrategy> strategyList) {
        for (com.antigravity.trading.strategy.v2.TradingStrategy strategy : strategyList) {
            strategies.put(strategy.getId(), strategy);
        }
    }

    public com.antigravity.trading.strategy.v2.TradingStrategy getStrategy(String id) {
        return strategies.get(id);
    }

    public List<Map<String, String>> getStrategyMetadata() {
        return strategies.values().stream()
                .map(s -> {
                    Map<String, String> meta = new HashMap<>();
                    meta.put("id", s.getId());
                    meta.put("name", s.getName());
                    try {
                        meta.put("defaultParams", new com.fasterxml.jackson.databind.ObjectMapper()
                                .writeValueAsString(s.getDefaultParams()));
                    } catch (Exception e) {
                        meta.put("defaultParams", "{}");
                    }
                    return meta;
                })
                .collect(Collectors.toList());
    }
}
