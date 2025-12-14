package com.antigravity.trading.util;

import com.antigravity.trading.domain.dto.CandleDto;
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

    public static List<Double> calculateAtr(List<CandleDto> candles, int period) {
        if (candles == null || candles.size() <= period) {
            return Collections.nCopies(candles == null ? 0 : candles.size(), null);
        }

        List<Double> atrValues = new ArrayList<>(Collections.nCopies(candles.size(), null));
        List<Double> trValues = new ArrayList<>(Collections.nCopies(candles.size(), 0.0));

        // Calculate True Range (TR)
        for (int i = 0; i < candles.size(); i++) {
            BigDecimal high = candles.get(i).getHigh();
            BigDecimal low = candles.get(i).getLow();
            BigDecimal prevClose = (i == 0) ? candles.get(0).getClose() : candles.get(i - 1).getClose();

            double hl = high.subtract(low).doubleValue();
            double hpc = Math.abs(high.subtract(prevClose).doubleValue());
            double lpc = Math.abs(low.subtract(prevClose).doubleValue());

            double tr = Math.max(hl, Math.max(hpc, lpc));
            trValues.set(i, tr);
        }

        // Initial ATR (Simple Moving Average of TR)
        double sumTr = 0.0;
        for (int i = 0; i < period; i++) {
            sumTr += trValues.get(i);
        }
        double initialAtr = sumTr / period;
        atrValues.set(period - 1, initialAtr); // Assuming definition starts at P-1

        // Smoothing ATR: [(Prior ATR x (n-1)) + Current TR] / n
        double prevAtr = initialAtr;
        for (int i = period; i < candles.size(); i++) {
            double currentTr = trValues.get(i);
            double currentAtr = (prevAtr * (period - 1) + currentTr) / period;
            atrValues.set(i, currentAtr);
            prevAtr = currentAtr;
        }

        return atrValues;
    }

    public static List<BigDecimal> calculateDonchianHigh(List<CandleDto> candles, int period) {
        if (candles == null || candles.size() < period) {
            return Collections.nCopies(candles == null ? 0 : candles.size(), null);
        }

        List<BigDecimal> results = new ArrayList<>(Collections.nCopies(candles.size(), null));

        // Donchian High of LAST N periods (excluding current for signal usually? or
        // including?)
        // Standard Donchian: Max of Highs in [i-N, i-1] usually for entry (breakout of
        // previous N days).
        // If we include 'i', it's just 'Highest High'.
        // Let's implement Max of [i-period+1, i] (Inclusive N periods)

        for (int i = period - 1; i < candles.size(); i++) {
            BigDecimal max = BigDecimal.ZERO;
            for (int j = 0; j < period; j++) {
                BigDecimal h = candles.get(i - j).getHigh();
                if (h.compareTo(max) > 0)
                    max = h;
            }
            results.set(i, max);
        }
        return results;
    }

    public static List<BigDecimal> calculateDonchianLow(List<CandleDto> candles, int period) {
        if (candles == null || candles.size() < period) {
            return Collections.nCopies(candles == null ? 0 : candles.size(), null);
        }

        List<BigDecimal> results = new ArrayList<>(Collections.nCopies(candles.size(), null));

        for (int i = period - 1; i < candles.size(); i++) {
            BigDecimal min = new BigDecimal("999999999");
            for (int j = 0; j < period; j++) {
                BigDecimal l = candles.get(i - j).getLow();
                if (l.compareTo(min) < 0)
                    min = l;
            }
            results.set(i, min);
        }
        return results;
    }

    public static List<BigDecimal> calculateSma(List<BigDecimal> prices, int period) {
        if (prices == null || prices.size() < period) {
            return Collections.nCopies(prices == null ? 0 : prices.size(), null);
        }

        List<BigDecimal> smaValues = new ArrayList<>(Collections.nCopies(prices.size(), null));
        BigDecimal sum = BigDecimal.ZERO;

        for (int i = 0; i < period; i++) {
            sum = sum.add(prices.get(i));
        }
        smaValues.set(period - 1, sum.divide(BigDecimal.valueOf(period), RoundingMode.HALF_UP));

        for (int i = period; i < prices.size(); i++) {
            sum = sum.subtract(prices.get(i - period)).add(prices.get(i));
            smaValues.set(i, sum.divide(BigDecimal.valueOf(period), RoundingMode.HALF_UP));
        }

        return smaValues;
    }

    public static List<Double> calculateVolumeRatio(List<BigDecimal> volumes, int period) {
        if (volumes == null || volumes.size() < period) {
            return Collections.nCopies(volumes == null ? 0 : volumes.size(), 0.0);
        }

        List<Double> ratios = new ArrayList<>(Collections.nCopies(volumes.size(), 0.0));

        // Optimize: maintain avg
        // Not optimized for brevity, simple sliding window
        for (int i = period; i < volumes.size(); i++) {
            double sum = 0;
            for (int j = 1; j <= period; j++) {
                sum += volumes.get(i - j).doubleValue();
            }
            double avg = sum / period;
            if (avg > 0) {
                ratios.set(i, volumes.get(i).doubleValue() / avg);
            }
        }
        return ratios;
    }
}
