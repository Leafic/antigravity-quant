package com.antigravity.trading.service;

import com.antigravity.trading.domain.entity.AutoTradeSchedule;

import com.antigravity.trading.repository.AutoTradeScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoTradeService {

    private final AutoTradeScheduleRepository repository;
    private final OrderService orderService;
    private final com.antigravity.trading.infrastructure.api.KisApiClient kisApiClient;

    // 1분마다 실행 (0초에)
    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void executeScheduledTrades() {
        LocalDateTime now = LocalDateTime.now();
        String currentTimeStr = now.format(DateTimeFormatter.ofPattern("HH:mm"));
        String currentAccount = kisApiClient.getAccountNo();

        log.debug("Checking auto-trade schedules for time: {} (Account: {})", currentTimeStr, currentAccount);

        if (currentAccount == null) return;

        List<AutoTradeSchedule> schedules = repository.findAllByAccountNo(currentAccount);

        for (AutoTradeSchedule schedule : schedules) {
            if (!Boolean.TRUE.equals(schedule.getIsActive())) continue;

            // 시간이 일치하고, 오늘 아직 실행되지 않았는지 확인 (단순화를 위해 마지막 실행 날짜 비교)
            // 주의: HH:mm 매칭이므로 하루에 한번만 실행되어야 함.
            if (schedule.getScheduleTime().equals(currentTimeStr)) {
                if (shouldExecute(schedule, now)) {
                    executeTrade(schedule);
                }
            }
        }
    }

    private boolean shouldExecute(AutoTradeSchedule schedule, LocalDateTime now) {
        if (schedule.getLastExecutedAt() == null) {
            return true;
        }
        // 이미 오늘 실행되었으면 스킵
        return !schedule.getLastExecutedAt().toLocalDate().isEqual(now.toLocalDate());
    }

    @Transactional
    public void executeTrade(AutoTradeSchedule schedule) {
        log.info("Executing scheduled trade: {} {} {} qty", schedule.getType(), schedule.getSymbol(), schedule.getQuantity());
        
        try {
            String type = schedule.getType().name(); // BUY or SELL
            java.math.BigDecimal price = java.math.BigDecimal.ZERO; // Market Price

            // Delegate to OrderService (Handles Execution, Logging, Notification)
            orderService.placeOrder(
                schedule.getSymbol(), 
                type, 
                price, 
                schedule.getQuantity(), 
                "AUTO_SCHEDULE", 
                "Dynamic Schedule Execution"
            );
            
            schedule.setLastExecutedAt(LocalDateTime.now());
            repository.save(schedule);

        } catch (Exception e) {
            log.error("Failed to execute scheduled trade for {}", schedule.getSymbol(), e);
            // Notification handled by OrderService for failure? 
            // OrderService throws error but also notifies "Order Failed".
            // So we just log here or maybe notify context-specific error if needed.
        }
    }

    public List<AutoTradeSchedule> getAllSchedules() {
        String currentAccount = kisApiClient.getAccountNo();
        return repository.findAllByAccountNo(currentAccount);
    }

    public AutoTradeSchedule createSchedule(AutoTradeSchedule schedule) {
        schedule.setAccountNo(kisApiClient.getAccountNo());
        return repository.save(schedule);
    }

    public AutoTradeSchedule updateSchedule(Long id, AutoTradeSchedule updated) {
        // TODO: Ensure ownership
        return repository.findById(id).map(existing -> {
            if (!existing.getScheduleTime().equals(updated.getScheduleTime())) {
                existing.setLastExecutedAt(null); // Reset execution flag if time changes
            }
            existing.setSymbol(updated.getSymbol());
            existing.setName(updated.getName());
            existing.setType(updated.getType());
            existing.setScheduleTime(updated.getScheduleTime());
            existing.setQuantity(updated.getQuantity());
            existing.setIsActive(updated.getIsActive());
            return repository.save(existing);
        }).orElseThrow(() -> new RuntimeException("Schedule not found"));
    }

    public void deleteSchedule(Long id) {
        repository.deleteById(id);
    }
}
