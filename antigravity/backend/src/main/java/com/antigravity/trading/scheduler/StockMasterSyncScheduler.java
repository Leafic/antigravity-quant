package com.antigravity.trading.scheduler;

import com.antigravity.trading.domain.entity.SchedulerHistory;
import com.antigravity.trading.repository.SchedulerHistoryRepository;
import com.antigravity.trading.service.StockMasterSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 종목 마스터 데이터 동기화 스케줄러
 * 매주 일요일 새벽 1시에 KOSPI/KOSDAQ 종목 마스터 데이터를 동기화합니다.
 * (주식 시장은 평일만 열리므로, 주말에 업데이트하면 충분합니다)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "scheduler.stock-master-sync.enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class StockMasterSyncScheduler {

    private final StockMasterSyncService stockMasterSyncService;
    private final SchedulerHistoryRepository schedulerHistoryRepository;

    /**
     * 매주 일요일 새벽 1시에 실행
     * 종목 정보는 자주 변경되지 않으므로 주 1회 동기화로 충분
     */
    @Scheduled(cron = "${scheduler.stock-master-sync.cron:0 0 1 * * SUN}")
    public void syncStockMasterData() {
        LocalDateTime startTime = LocalDateTime.now();

        // 히스토리 생성
        SchedulerHistory history = SchedulerHistory.builder()
                .jobName("STOCK_MASTER_SYNC")
                .startTime(startTime)
                .status("RUNNING")
                .build();
        schedulerHistoryRepository.save(history);

        log.info("========================================");
        log.info("Starting scheduled stock master synchronization (History ID: {})", history.getId());
        log.info("========================================");

        try {
            // 종목 마스터 데이터 동기화 (KOSPI + KOSDAQ)
            StockMasterSyncService.SyncResult result = stockMasterSyncService.syncAllMarkets();

            // 성공 기록
            history.setEndTime(LocalDateTime.now());
            history.setStatus(result.isSuccess() ? "SUCCESS" : "FAILED");
            history.setMessage(result.getMessage());
            history.setTotalItems(result.getTotalCount());
            history.setSuccessItems(result.getTotalCount());
            history.setFailedItems(0);
            schedulerHistoryRepository.save(history);

            log.info("========================================");
            log.info("Stock master sync completed successfully");
            log.info("Total stocks synced: {} (KOSPI: {}, KOSDAQ: {})",
                    result.getTotalCount(), result.getKospiCount(), result.getKosdaqCount());
            log.info("========================================");

        } catch (Exception e) {
            // 실패 기록
            history.setEndTime(LocalDateTime.now());
            history.setStatus("FAILED");
            history.setMessage("Stock master sync failed: " + e.getMessage());
            history.setErrorDetails(getStackTrace(e));
            schedulerHistoryRepository.save(history);

            log.error("========================================");
            log.error("Stock master sync failed: {}", e.getMessage(), e);
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
    public StockMasterSyncService.SyncResult triggerManualSync() {
        log.info("Manual stock master sync triggered");

        try {
            StockMasterSyncService.SyncResult result = stockMasterSyncService.syncAllMarkets();
            log.info("Manual stock master sync completed: {}", result.getMessage());
            return result;

        } catch (Exception e) {
            log.error("Manual stock master sync failed: {}", e.getMessage(), e);
            throw new RuntimeException("Manual stock master sync failed", e);
        }
    }
}
