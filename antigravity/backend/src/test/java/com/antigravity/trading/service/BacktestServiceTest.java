package com.antigravity.trading.service;

import com.antigravity.trading.domain.entity.BacktestRun;
import com.antigravity.trading.domain.entity.DecisionLog;
import com.antigravity.trading.engine.StrategyEngine;
import com.antigravity.trading.engine.model.MarketEvent;
import com.antigravity.trading.engine.model.Signal;
import com.antigravity.trading.engine.model.StrategyContext;
import com.antigravity.trading.infrastructure.api.KisApiClient;
import com.antigravity.trading.infrastructure.api.dto.KisChartResponse;
import com.antigravity.trading.repository.BacktestRunRepository;
import com.antigravity.trading.repository.DecisionLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BacktestServiceTest {

    @Mock
    private KisApiClient kisApiClient;
    @Mock
    private StrategyEngine strategyEngine;
    @Mock
    private BacktestRunRepository backtestRunRepository;
    @Mock
    private DecisionLogRepository decisionLogRepository;

    private BacktestService backtestService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        backtestService = new BacktestService(kisApiClient, strategyEngine, backtestRunRepository,
                decisionLogRepository);
    }

    @Test
    void runBacktest_ShouldExecuteStrategyAndLogDecisions() {
        // Arrange
        String symbol = "005930";
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        LocalDateTime end = LocalDateTime.now();

        // Mock KIS Response (Need at least 21 candles for MA20 + 1)
        KisChartResponse response = new KisChartResponse();
        KisChartResponse.Output2 output = new KisChartResponse.Output2();
        output.setStckBsopDate("20230101");
        output.setStckClpr("10000");
        output.setStckHgpr("11000");
        output.setStckLwpr("9000");
        output.setStckOprc("9500");
        output.setAcmlVol("100000");

        List<KisChartResponse.Output2> outputs = Collections.nCopies(30, output); // 30 candles
        response.setOutput2(outputs);

        when(kisApiClient.getDailyChart(any(), any(), any())).thenReturn(response);
        when(backtestRunRepository.save(any(BacktestRun.class))).thenAnswer(i -> {
            BacktestRun run = i.getArgument(0);
            run.setId(1L);
            return run;
        });

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
