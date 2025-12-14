package com.antigravity.trading.service;

import com.antigravity.trading.domain.entity.SchedulerHistory;
import com.antigravity.trading.domain.entity.StockMaster;
import com.antigravity.trading.infrastructure.api.KisApiClient;
import com.antigravity.trading.infrastructure.api.dto.KisStockMasterDto;
import com.antigravity.trading.repository.SchedulerHistoryRepository;
import com.antigravity.trading.repository.StockMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 종목 마스터 데이터 동기화 서비스
 * KIS 다운로드 서버에서 KOSPI/KOSDAQ 종목 정보를 다운로드하여 DB에 저장합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockMasterSyncService {

    private final KisApiClient kisApiClient;
    private final StockMasterRepository stockMasterRepository;
    private final SchedulerHistoryRepository schedulerHistoryRepository;

    /**
     * 모든 시장(KOSPI, KOSDAQ)의 종목 마스터 데이터 동기화 (수동 실행)
     */
    @Transactional
    public SyncResult syncAllMarkets() {
        return syncAllMarkets(false);
    }

    /**
     * 모든 시장(KOSPI, KOSDAQ)의 종목 마스터 데이터 동기화
     * @param scheduled 스케줄러에 의한 자동 실행 여부
     */
    @Transactional
    public SyncResult syncAllMarkets(boolean scheduled) {
        log.info("========================================");
        log.info("Starting stock master data synchronization for all markets ({})",
                scheduled ? "Scheduled" : "Manual");
        log.info("========================================");

        SyncResult result = new SyncResult();
        LocalDateTime startTime = LocalDateTime.now();

        // 히스토리 기록 시작
        SchedulerHistory history = new SchedulerHistory();
        history.setJobName(scheduled ? "STOCK_MASTER_SYNC_AUTO" : "STOCK_MASTER_SYNC_MANUAL");
        history.setStartTime(startTime);
        history.setStatus("RUNNING");
        history = schedulerHistoryRepository.save(history);

        try {
            // KOSPI 동기화
            log.info("Syncing KOSPI stock master data...");
            int kospiCount = syncMarket("KOSPI");
            result.kospiCount = kospiCount;
            log.info("✓ KOSPI sync completed: {} stocks", kospiCount);

            // KOSDAQ 동기화
            log.info("Syncing KOSDAQ stock master data...");
            int kosdaqCount = syncMarket("KOSDAQ");
            result.kosdaqCount = kosdaqCount;
            log.info("✓ KOSDAQ sync completed: {} stocks", kosdaqCount);

            result.success = true;
            result.totalCount = kospiCount + kosdaqCount;
            result.message = String.format("Successfully synced %d stocks (KOSPI: %d, KOSDAQ: %d)",
                    result.totalCount, kospiCount, kosdaqCount);

            log.info("========================================");
            log.info("Stock master sync completed successfully");
            log.info("Total: {} stocks (KOSPI: {}, KOSDAQ: {})", result.totalCount, kospiCount, kosdaqCount);
            log.info("Duration: {} seconds", java.time.Duration.between(startTime, LocalDateTime.now()).getSeconds());
            log.info("========================================");

            // 히스토리 성공 기록
            history.setStatus("SUCCESS");
            history.setEndTime(LocalDateTime.now());
            history.setMessage(result.message);
            schedulerHistoryRepository.save(history);

        } catch (Exception e) {
            result.success = false;
            result.message = "Stock master sync failed: " + e.getMessage();
            result.error = e.getMessage();

            log.error("========================================");
            log.error("Stock master sync failed: {}", e.getMessage(), e);
            log.error("========================================");

            // 히스토리 실패 기록
            history.setStatus("FAILED");
            history.setEndTime(LocalDateTime.now());
            history.setMessage(e.getMessage());
            schedulerHistoryRepository.save(history);
        }

        return result;
    }

    /**
     * 특정 시장의 종목 마스터 데이터 동기화
     */
    @Transactional
    public int syncMarket(String marketType) {
        log.info("Downloading {} stock master data from KIS server...", marketType);

        // 1. KIS 서버에서 마스터 데이터 다운로드 및 파싱
        List<KisStockMasterDto> stockDtos = kisApiClient.downloadStockMasterData(marketType);

        log.info("Downloaded {} {} stocks, saving to database...", stockDtos.size(), marketType);

        // 2. DTO -> Entity 변환 및 DB 저장
        LocalDateTime now = LocalDateTime.now();
        int savedCount = 0;

        for (KisStockMasterDto dto : stockDtos) {
            try {
                StockMaster entity = convertToEntity(dto, now);
                stockMasterRepository.save(entity);
                savedCount++;
            } catch (Exception e) {
                log.warn("Failed to save stock {}: {}", dto.getSymbol(), e.getMessage());
            }
        }

        log.info("Saved {} {} stocks to database", savedCount, marketType);
        return savedCount;
    }

    /**
     * DTO를 Entity로 변환
     */
    private StockMaster convertToEntity(KisStockMasterDto dto, LocalDateTime updateTime) {
        return StockMaster.builder()
                .code(dto.getSymbol())
                .name(dto.getName())
                .market(dto.getMarketType())
                .sector(dto.getSector())
                .industry(dto.getIndustry())
                .listingDate(dto.getListingDate())
                .isManaged(dto.getIsManaged())
                .isSuspended(dto.getIsSuspended())
                .lastUpdated(updateTime)
                .build();
    }

    /**
     * 동기화 결과 DTO
     */
    public static class SyncResult {
        public boolean success;
        public String message;
        public String error;
        public int totalCount;
        public int kospiCount;
        public int kosdaqCount;

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getError() {
            return error;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public int getKospiCount() {
            return kospiCount;
        }

        public int getKosdaqCount() {
            return kosdaqCount;
        }
    }
}
