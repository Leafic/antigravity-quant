package com.antigravity.trading.service;

import com.antigravity.trading.infrastructure.api.KisApiClient;
import com.antigravity.trading.infrastructure.api.dto.KisConditionSearchListResponse;
import com.antigravity.trading.infrastructure.api.dto.KisConditionSearchResultResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 한국투자증권 조건검색 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConditionSearchService {

    private final KisApiClient kisApiClient;

    @Value("${kis.user-id:#{null}}")
    private String defaultUserId;

    /**
     * 조건검색 목록 조회
     */
    public List<KisConditionSearchListResponse.ConditionItem> getConditionList(String userId) {
        String targetUserId = (userId != null) ? userId : defaultUserId;

        if (targetUserId == null) {
            throw new IllegalArgumentException("User ID is required. Set kis.user-id in application.yml or provide userId parameter.");
        }

        log.info("Fetching condition search list for user: {}", targetUserId);

        KisConditionSearchListResponse response = kisApiClient.getConditionSearchList(targetUserId);

        if (response == null || response.getOutput2() == null) {
            log.warn("No condition search list found for user: {}", targetUserId);
            return List.of();
        }

        log.info("Found {} condition searches", response.getOutput2().size());
        return response.getOutput2();
    }

    /**
     * 조건검색 종목 조회
     */
    public List<KisConditionSearchResultResponse.StockItem> searchStocks(String userId, String seq) {
        String targetUserId = (userId != null) ? userId : defaultUserId;

        if (targetUserId == null) {
            throw new IllegalArgumentException("User ID is required");
        }

        log.info("Fetching stocks for condition search - user: {}, seq: {}", targetUserId, seq);

        KisConditionSearchResultResponse response = kisApiClient.getConditionSearchResult(targetUserId, seq);

        if (response == null || response.getOutput2() == null) {
            log.warn("No stocks found for condition - user: {}, seq: {}", targetUserId, seq);
            return List.of();
        }

        log.info("Found {} stocks matching condition", response.getOutput2().size());
        return response.getOutput2();
    }

    /**
     * 조건검색으로 종목 코드 목록만 추출
     */
    public List<String> getStockCodes(String userId, String seq) {
        List<KisConditionSearchResultResponse.StockItem> stocks = searchStocks(userId, seq);

        return stocks.stream()
                .map(KisConditionSearchResultResponse.StockItem::getStockCode)
                .toList();
    }
}
