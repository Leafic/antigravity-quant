package com.antigravity.trading.scheduler;

import com.antigravity.trading.domain.entity.SchedulerHistory;
import com.antigravity.trading.repository.SchedulerHistoryRepository;
import com.antigravity.trading.service.DataPipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 데이터 수집 스케줄러
 * 매일 새벽 2시에 모든 종목의 일봉 데이터를 수집하여 DB에 저장
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "scheduler.data-collection.enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class DataCollectionScheduler {

    private final DataPipelineService dataPipelineService;
    private final SchedulerHistoryRepository schedulerHistoryRepository;

    /**
     * 매일 새벽 2시에 실행
     * - 주말/공휴일에도 실행되지만, KIS API에서 데이터가 없으면 스킵됨
     */
    @Scheduled(cron = "${scheduler.data-collection.cron:0 0 2 * * *}")
    public void collectDailyData() {
        LocalDateTime startTime = LocalDateTime.now();

        // 히스토리 생성
        SchedulerHistory history = SchedulerHistory.builder()
                .jobName("DATA_COLLECTION")
                .startTime(startTime)
                .status("RUNNING")
                .build();
        schedulerHistoryRepository.save(history);

        log.info("========================================");
        log.info("Starting scheduled daily data collection (History ID: {})", history.getId());
        log.info("========================================");

        try {
            // 스케줄링된 종목만 데이터 수집 (활성화된 종목만)
            DataPipelineService.CollectionResult result = dataPipelineService.collectScheduledStocks(100);

            // 보조지표 계산
            log.info("Calculating technical indicators for all stocks");
            dataPipelineService.calculateAllIndicators();

            // 성공 기록
            history.setEndTime(LocalDateTime.now());
            history.setStatus("SUCCESS");

            String message = String.format(
                "Data collection completed: %d/%d stocks (Period: %s ~ %s)",
                result.getSuccessCount(), result.getTotalStocks(),
                result.getStartDate().toLocalDate(), result.getEndDate().toLocalDate()
            );

            if (!result.getFailedSymbols().isEmpty()) {
                message += "\nFailed: " + String.join(", ", result.getFailedSymbols());
            }

            history.setMessage(message);
            schedulerHistoryRepository.save(history);

            log.info("========================================");
            log.info("Daily data collection completed successfully");
            log.info("========================================");

        } catch (Exception e) {
            // 실패 기록
            history.setEndTime(LocalDateTime.now());
            history.setStatus("FAILED");
            history.setMessage("Data collection failed: " + e.getMessage());
            history.setErrorDetails(getStackTrace(e));
            schedulerHistoryRepository.save(history);

            log.error("========================================");
            log.error("Daily data collection failed: {}", e.getMessage(), e);
            log.error("========================================");
        }
    }

    /**
     * 스택 트레이스를 문자열로 변환
     */
    private String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getClass().getName()).append(": ").append(e.getMessage()).append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
            if (sb.length() > 4500) { // DB 컬럼 제한 고려
                sb.append("\n... (truncated)");
                break;
            }
        }
        return sb.toString();
    }

    /**
     * 수동 트리거용 (테스트/디버깅)
     * API 엔드포인트에서 호출 가능
     */
    public void triggerManualCollection(int days) {
        log.info("Manual data collection triggered for last {} days", days);

        try {
            dataPipelineService.collectAllStockData(days);
            dataPipelineService.calculateAllIndicators();

            log.info("Manual data collection completed");

        } catch (Exception e) {
            log.error("Manual data collection failed: {}", e.getMessage(), e);
            throw new RuntimeException("Manual data collection failed", e);
        }
    }

    /**
     * 특정 종목만 수동 수집
     */
    public void triggerStockCollection(String symbol, int days) {
        log.info("Manual data collection triggered for {} (last {} days)", symbol, days);

        try {
            dataPipelineService.collectStockData(symbol, days);
            dataPipelineService.calculateIndicators(symbol);

            log.info("Manual data collection completed for {}", symbol);

        } catch (Exception e) {
            log.error("Manual data collection failed for {}: {}", symbol, e.getMessage(), e);
            throw new RuntimeException("Manual data collection failed for " + symbol, e);
        }
    }
}
