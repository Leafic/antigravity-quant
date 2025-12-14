package com.antigravity.trading.service;

import com.antigravity.trading.domain.entity.BacktestRun;
import com.antigravity.trading.domain.entity.CandleHistory;
import com.antigravity.trading.domain.entity.DecisionLog;
import com.antigravity.trading.engine.StrategyEngine;
import com.antigravity.trading.engine.StrategyRegistry;
import com.antigravity.trading.engine.model.MarketEvent;
import com.antigravity.trading.engine.model.Signal;
import com.antigravity.trading.engine.model.StrategyContext;
import com.antigravity.trading.infrastructure.api.KisApiClient;
import com.antigravity.trading.infrastructure.api.dto.KisChartResponse;
import com.antigravity.trading.repository.BacktestRunRepository;
import com.antigravity.trading.repository.CandleHistoryRepository;
import com.antigravity.trading.repository.DecisionLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class BacktestServiceTest {

    @Mock
    private KisApiClient kisApiClient;
    @Mock
    private StrategyRegistry strategyRegistry;
    @Mock
    private StrategyEngine strategyEngine;
    @Mock
    private ReasonCodeMapper reasonMapper;
    @Mock
    private BacktestRunRepository backtestRunRepository;
    @Mock
    private DecisionLogRepository decisionLogRepository;
    @Mock
    private CandleHistoryRepository candleHistoryRepository;

    private BacktestService backtestService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        backtestService = new BacktestService(kisApiClient, strategyRegistry, reasonMapper,
                backtestRunRepository, decisionLogRepository, candleHistoryRepository);
    }

    @Test
    void runBacktest_ShouldExecuteStrategyAndLogDecisions() {
        // Arrange
        String symbol = "005930";
        LocalDateTime start = LocalDateTime.of(2023, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2023, 1, 31, 23, 59);

        // Mock DB Response - Empty to fallback to API
        when(candleHistoryRepository.findBySymbolAndTimeBetween(anyString(), any(), any()))
                .thenReturn(new ArrayList<>());

        // Mock KIS Response (Need at least 21 candles for MA20 + 1)
        // Create 30 candles with different dates within range
        KisChartResponse response = new KisChartResponse();
        List<KisChartResponse.Output2> outputs = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            KisChartResponse.Output2 output = new KisChartResponse.Output2();
            output.setStckBsopDate(String.format("202301%02d", i)); // 20230101 ~ 20230130
            output.setStckClpr("10000");
            output.setStckHgpr("11000");
            output.setStckLwpr("9000");
            output.setStckOprc("9500");
            output.setAcmlVol("100000");
            outputs.add(output);
        }
        response.setOutput2(outputs);

        when(kisApiClient.getDailyChart(any(), any(), any())).thenReturn(response);
        when(backtestRunRepository.save(any(BacktestRun.class))).thenAnswer(i -> {
            BacktestRun run = i.getArgument(0);
            run.setId(1L);
            return run;
        });

        // Mock Strategy Registry to return our mock strategyEngine
        when(strategyRegistry.getStrategy(anyString())).thenReturn(strategyEngine);

        // Mock Strategy Signal
        when(strategyEngine.analyze(any(MarketEvent.class), any(StrategyContext.class)))
                .thenReturn(Signal.builder().type(Signal.Type.BUY).reasonCode("TEST").reasonDetail("Fake Buy").build());

        // Act
        backtestService.runBacktest(symbol, start, end);

        // Assert
        verify(strategyEngine, atLeastOnce()).analyze(any(), any());
        verify(decisionLogRepository, atLeastOnce()).save(any(DecisionLog.class));
        verify(backtestRunRepository, times(2)).save(any(BacktestRun.class)); // Start and End
    }
}
