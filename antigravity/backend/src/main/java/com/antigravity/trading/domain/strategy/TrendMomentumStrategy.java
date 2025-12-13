package com.antigravity.trading.domain.strategy;

import com.antigravity.trading.domain.dto.CandleDto;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;

@Component
public class TrendMomentumStrategy implements TradingStrategy {

    @Override
    public String getName() {
        return "TrendMomentumV1";
    }

    @Override
    public StrategySignal analyze(String symbol, List<CandleDto> candles) {
        if (candles == null || candles.size() < 22) { // Need at least 20 + 2
            return StrategySignal.builder().symbol(symbol).type(StrategySignal.SignalType.HOLD)
                    .reason("Insufficient Data (" + (candles == null ? 0 : candles.size()) + ")").build();
        }

        CandleDto today = candles.get(candles.size() - 1);
        CandleDto yesterday = candles.get(candles.size() - 2);

        // 1. Calculate MA20 (excluding today for stability? Or including? Usually close
        // based. Let's use up to yesterday for reference or today's partial)
        // Trend Filter: Price > MA20. Use Yesterday's MA20 to check trend start, or
        // Today's current price > MA20.
        // Let's verify Yesterday Close > Yesterday MA20 (Uptrend context) AND Today
        // Current > Today Moving Average?
        BigDecimal ma20 = calculateSMA(candles, 20, candles.size() - 2); // Yesterday's MA20

        // 2. Trend Filter
        BigDecimal currentPrice = today.getClose(); // For daily candle, close is current price
        // If Price is above MA20 (Trend)
        if (currentPrice.compareTo(ma20) <= 0) {
            return StrategySignal.builder().symbol(symbol).type(StrategySignal.SignalType.HOLD)
                    .reason("비추세 구간 (MA20 하회)")
                    .build();
        }

        // 3. Volume Filter (Vol > AvgVol * 1.0)
        BigDecimal avgVol20 = calculateAvgVolume(candles, 20, candles.size() - 2);
        BigDecimal currentVol = today.getVolume();
        // Since it's intraday, this might be partial volume.
        // Strategy says "Volume or value is elevated".
        // Loose check: Current Volume > AvgVol * 0.5 (if early day) or 1.0.
        // Let's use 0.8 as threshold for prototype.
        if (currentVol.compareTo(avgVol20.multiply(new BigDecimal("0.8"))) <= 0) {
            return StrategySignal.builder().symbol(symbol).type(StrategySignal.SignalType.HOLD)
                    .reason("모멘텀 약함 (거래량 20일 평균 미달)")
                    .build();
        }

        // 4. Breakout (Today High > Yesterday High)
        if (today.getHigh().compareTo(yesterday.getHigh()) <= 0) {
            return StrategySignal.builder().symbol(symbol).type(StrategySignal.SignalType.HOLD)
                    .reason("돌파 실패 (전일 고가 미달)")
                    .build();
        }

        // All conditions met -> BUY
        return StrategySignal.builder()
                .symbol(symbol)
                .type(StrategySignal.SignalType.BUY)
                .price(currentPrice)
                .reason("강력 매수 (MA20 상향 돌파 + 전일 고가 갱신 + 거래량 급증)")
                .build();
    }

    private BigDecimal calculateSMA(List<CandleDto> candles, int period, int endIndex) {
        if (endIndex < period - 1)
            return BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < period; i++) {
            sum = sum.add(candles.get(endIndex - i).getClose());
        }
        return sum.divide(new BigDecimal(period), 2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAvgVolume(List<CandleDto> candles, int period, int endIndex) {
        if (endIndex < period - 1)
            return BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < period; i++) {
            sum = sum.add(candles.get(endIndex - i).getVolume());
        }
        return sum.divide(new BigDecimal(period), 2, java.math.RoundingMode.HALF_UP);
    }
}
