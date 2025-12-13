package com.antigravity.trading.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TechnicalIndicators {

    public static List<Double> calculateRsi(List<BigDecimal> prices, int period) {
        if (prices == null || prices.size() <= period) {
            return Collections.nCopies(prices == null ? 0 : prices.size(), null);
        }

        List<Double> rsiValues = new ArrayList<>(Collections.nCopies(prices.size(), null));

        double avgGain = 0.0;
        double avgLoss = 0.0;

        // Initial Average
        for (int i = 1; i <= period; i++) {
            double change = prices.get(i).subtract(prices.get(i - 1)).doubleValue();
            if (change > 0)
                avgGain += change;
            else
                avgLoss += Math.abs(change);
        }
        avgGain /= period;
        avgLoss /= period;

        double rs = (avgLoss == 0) ? 100.0 : avgGain / avgLoss;
        double rsi = 100.0 - (100.0 / (1.0 + rs));
        rsiValues.set(period, rsi);

        // Smoothing
        for (int i = period + 1; i < prices.size(); i++) {
            double change = prices.get(i).subtract(prices.get(i - 1)).doubleValue();
            double gain = (change > 0) ? change : 0.0;
            double loss = (change > 0) ? 0.0 : Math.abs(change);

            avgGain = (avgGain * (period - 1) + gain) / period;
            avgLoss = (avgLoss * (period - 1) + loss) / period;

            rs = (avgLoss == 0) ? 100.0 : avgGain / avgLoss;
            rsi = 100.0 - (100.0 / (1.0 + rs));
            rsiValues.set(i, rsi);
        }

        return rsiValues;
    }
}
