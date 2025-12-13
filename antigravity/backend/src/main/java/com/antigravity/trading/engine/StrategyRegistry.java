package com.antigravity.trading.engine;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class StrategyRegistry {

    private final Map<String, StrategyEngine> strategies = new HashMap<>();

    public StrategyRegistry(List<StrategyEngine> strategyList) {
        for (StrategyEngine strategy : strategyList) {
            strategies.put(strategy.getId(), strategy);
        }
    }

    public StrategyEngine getStrategy(String id) {
        return strategies.get(id);
    }

    public List<Map<String, String>> getStrategyMetadata() {
        return strategies.values().stream()
                .map(s -> {
                    Map<String, String> meta = new HashMap<>();
                    meta.put("id", s.getId());
                    meta.put("name", s.getName());
                    meta.put("description", s.getDescription());
                    meta.put("defaultParams", s.getDefaultParamsJson());
                    return meta;
                })
                .collect(Collectors.toList());
    }
}
