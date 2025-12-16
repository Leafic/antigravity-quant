package com.antigravity.trading.scheduler;

import com.antigravity.trading.domain.entity.AutoTradeSchedule;
import com.antigravity.trading.repository.AutoTradeScheduleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Legacy Scheduler Migrator
 * Initializes default schedules into the database for dynamic execution.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GenomeAutoTradeScheduler {

    private final AutoTradeScheduleRepository repository;

    private static final String SYMBOL = "314130";
    private static final String NAME = "지놈앤컴퍼니";
    private static final int QTY = 10;

    @PostConstruct
    public void initDefaultSchedules() {
        log.info("[Scheduler Migrator] Checking default schedules...");

        // 1. Buy at Open (09:00:05) -> Dynamic: 09:00
        if (repository.findBySymbolAndScheduleTime(SYMBOL, "09:00").isEmpty()) {
            AutoTradeSchedule schedule = AutoTradeSchedule.builder()
                    .symbol(SYMBOL)
                    .name(NAME)
                    .type(AutoTradeSchedule.TradeType.BUY)
                    .scheduleTime("09:00")
                    .quantity(QTY)
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            repository.save(schedule);
            log.info("Initialized default BUY schedule for {} at 09:00", SYMBOL);
        }

        // 2. Buy at Close (15:19:00) - Previously Sell, then Buy, now Dynamic
        if (repository.findBySymbolAndScheduleTime(SYMBOL, "15:19").isEmpty()) {
            AutoTradeSchedule schedule = AutoTradeSchedule.builder()
                    .symbol(SYMBOL)
                    .name(NAME)
                    .type(AutoTradeSchedule.TradeType.BUY)
                    .scheduleTime("15:19") // 15:19 Trigger
                    .quantity(QTY)
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            repository.save(schedule);
            log.info("Initialized default BUY (Accumulation) schedule for {} at 15:19", SYMBOL);
        }
    }
}

