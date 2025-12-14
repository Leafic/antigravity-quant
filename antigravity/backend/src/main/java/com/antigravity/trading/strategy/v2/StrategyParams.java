package com.antigravity.trading.strategy.v2;

import lombok.Data;
import com.fasterxml.jackson.databind.ObjectMapper;

@Data
public abstract class StrategyParams {
    // Common parameters if any, e.g. timeframe
    protected String timeframe = "30m";

    public static <T extends StrategyParams> T fromJson(String json, Class<T> clazz) {
        try {
            if (json == null || json.isEmpty())
                return clazz.getDeclaredConstructor().newInstance();
            return new ObjectMapper().readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse params", e);
        }
    }
}
