package com.antigravity.trading.controller;

import com.antigravity.trading.domain.entity.SchedulerHistory;
import com.antigravity.trading.repository.SchedulerHistoryRepository;
import com.antigravity.trading.scheduler.StockMasterSyncScheduler;
import com.antigravity.trading.service.StockMasterSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 스케줄러 모니터링 및 관리 API
 */
@Slf4j
@RestController
@RequestMapping("/api/scheduler")
@RequiredArgsConstructor
public class SchedulerManagementController {

    private final SchedulerHistoryRepository schedulerHistoryRepository;
    private final StockMasterSyncService stockMasterSyncService;

    @Autowired(required = false)
    private StockMasterSyncScheduler stockMasterSyncScheduler;

    @Value("${scheduler.data-collection.enabled:false}")
    private boolean schedulerEnabled;

    @Value("${scheduler.data-collection.cron:0 0 2 * * *}")
    private String cronExpression;

    /**
     * 스케줄러 상태 조회
     * GET /api/scheduler/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSchedulerStatus() {
        Map<String, Object> status = new HashMap<>();

        // 스케줄러 활성화 상태
        status.put("enabled", schedulerEnabled);
        status.put("cronExpression", cronExpression);
        status.put("nextScheduledTime", getNextScheduledTime());

        // 마지막 실행 정보
        Optional<SchedulerHistory> lastRun = schedulerHistoryRepository
                .findFirstByJobNameOrderByStartTimeDesc("DATA_COLLECTION");

        if (lastRun.isPresent()) {
            SchedulerHistory history = lastRun.get();
            Map<String, Object> lastRunInfo = new HashMap<>();
            lastRunInfo.put("id", history.getId());
            lastRunInfo.put("startTime", history.getStartTime());
            lastRunInfo.put("endTime", history.getEndTime());
            lastRunInfo.put("status", history.getStatus());
            lastRunInfo.put("message", history.getMessage());
            lastRunInfo.put("duration", calculateDuration(history));

            status.put("lastRun", lastRunInfo);
        } else {
            status.put("lastRun", null);
        }

        // 실행 중인 Job 확인
        List<SchedulerHistory> runningJobs = schedulerHistoryRepository.findByStatus("RUNNING");
        status.put("isRunning", !runningJobs.isEmpty());
        status.put("runningJobs", runningJobs.size());

        // 통계
        long totalRuns = schedulerHistoryRepository.countTotalByJobName("DATA_COLLECTION");
        long successRuns = schedulerHistoryRepository.countSuccessByJobName("DATA_COLLECTION");
        double successRate = totalRuns > 0 ? (successRuns * 100.0 / totalRuns) : 0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRuns", totalRuns);
        stats.put("successRuns", successRuns);
        stats.put("failedRuns", totalRuns - successRuns);
        stats.put("successRate", String.format("%.1f%%", successRate));

        status.put("statistics", stats);

        return ResponseEntity.ok(status);
    }

    /**
     * 스케줄러 실행 히스토리 조회
     * GET /api/scheduler/history?limit=50
     */
    @GetMapping("/history")
    public ResponseEntity<List<SchedulerHistory>> getSchedulerHistory(
            @RequestParam(defaultValue = "50") int limit) {

        List<SchedulerHistory> history = schedulerHistoryRepository
                .findByOrderByStartTimeDesc(PageRequest.of(0, limit));

        return ResponseEntity.ok(history);
    }

    /**
     * 특정 실행 히스토리 상세 조회
     * GET /api/scheduler/history/{id}
     */
    @GetMapping("/history/{id}")
    public ResponseEntity<SchedulerHistory> getSchedulerHistoryDetail(@PathVariable Long id) {
        Optional<SchedulerHistory> history = schedulerHistoryRepository.findById(id);

        return history.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 기간별 히스토리 조회
     * GET /api/scheduler/history/range?start=2024-01-01T00:00:00&end=2024-12-31T23:59:59
     */
    @GetMapping("/history/range")
    public ResponseEntity<List<SchedulerHistory>> getSchedulerHistoryByRange(
            @RequestParam String start,
            @RequestParam String end) {

        LocalDateTime startTime = LocalDateTime.parse(start);
        LocalDateTime endTime = LocalDateTime.parse(end);

        List<SchedulerHistory> history = schedulerHistoryRepository
                .findByStartTimeBetweenOrderByStartTimeDesc(startTime, endTime);

        return ResponseEntity.ok(history);
    }

    /**
     * 스케줄러 설정 정보
     * GET /api/scheduler/config
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getSchedulerConfig() {
        Map<String, Object> config = new HashMap<>();

        config.put("enabled", schedulerEnabled);
        config.put("cronExpression", cronExpression);
        config.put("humanReadable", parseCronExpression(cronExpression));
        config.put("timezone", "Asia/Seoul");

        return ResponseEntity.ok(config);
    }

    /**
     * 다음 실행 예정 시간 계산 (간단한 버전)
     */
    private String getNextScheduledTime() {
        if (!schedulerEnabled) {
            return "Scheduler is disabled";
        }

        // Cron: "0 0 2 * * *" = 매일 새벽 2시
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.withHour(2).withMinute(0).withSecond(0);

        if (now.getHour() >= 2) {
            // 오늘 2시가 지났으면 내일 2시
            nextRun = nextRun.plusDays(1);
        }

        return nextRun.toString();
    }

    /**
     * Cron 표현식을 사람이 읽기 쉬운 형식으로 변환
     */
    private String parseCronExpression(String cron) {
        if ("0 0 2 * * *".equals(cron)) {
            return "Every day at 2:00 AM";
        }
        return cron;
    }

    /**
     * 종목 마스터 데이터 수동 동기화
     * POST /api/scheduler/sync-stock-master
     */
    @PostMapping("/sync-stock-master")
    public ResponseEntity<Map<String, Object>> syncStockMaster() {
        log.info("Manual stock master sync triggered via API");

        Map<String, Object> response = new HashMap<>();

        try {
            // 스케줄러 없이 서비스 직접 호출
            StockMasterSyncService.SyncResult result = stockMasterSyncService.syncAllMarkets();

            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("totalCount", result.getTotalCount());
            response.put("kospiCount", result.getKospiCount());
            response.put("kosdaqCount", result.getKosdaqCount());

            if (!result.isSuccess()) {
                response.put("error", result.getError());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Manual stock master sync failed: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Stock master sync failed");
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 실행 시간 계산 (초 단위)
     */
    private Long calculateDuration(SchedulerHistory history) {
        if (history.getEndTime() == null) {
            return null;
        }

        return java.time.Duration.between(
                history.getStartTime(),
                history.getEndTime()
        ).getSeconds();
    }
}
