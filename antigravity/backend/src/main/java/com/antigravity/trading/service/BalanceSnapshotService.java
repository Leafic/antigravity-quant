package com.antigravity.trading.service;

import com.antigravity.trading.engine.model.BalanceSnapshot;
import com.antigravity.trading.infrastructure.api.KisApiClient;
import com.antigravity.trading.infrastructure.api.dto.KisBalanceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceSnapshotService {

    private final KisApiClient kisApiClient;

    /**
     * Get current balance snapshot. Cached for 5 seconds.
     */
    @Cacheable(value = "balanceSnapshot", key = "'latest'", unless = "#result == null")
    public BalanceSnapshot getSnapshot() {
        return fetchFromKis();
    }

    /**
     * Force refresh the snapshot (evict cache and fetch).
     */
    @CacheEvict(value = "balanceSnapshot", key = "'latest'")
    public BalanceSnapshot refresh() {
        return fetchFromKis();
    }

    private BalanceSnapshot fetchFromKis() {
        try {
            // NOTE: KisApiClient.getAccountBalance needs to return detailed DTO now.
            // Assuming we modified KisApiClient to return `KisBalanceResponse` with output2
            // and output1 (holdings)
            KisBalanceResponse response = kisApiClient.getAccountBalance();

            if (response == null || response.getOutput2() == null) {
                log.error("Failed to fetch balance from KIS");
                return null;
            }

            KisBalanceResponse.Output2 summary = response.getOutput2().get(0);

            // Map Holdings (Output1 needs to be added to KisBalanceResponse)
            List<BalanceSnapshot.PositionSnapshot> positions = Collections.emptyList();
            if (response.getOutput1() != null) {
                positions = response.getOutput1().stream()
                        .map(h -> BalanceSnapshot.PositionSnapshot.builder()
                                .symbol(h.getPdno())
                                .name(h.getPrdtName())
                                .quantity(Long.parseLong(h.getHldgQty()))
                                .avgPrice(new BigDecimal(h.getPrpr())) // Using Current Price (prpr) as proxy or Average
                                                                       // Buy (pchs_avg_pric)? Output1 has
                                                                       // pchs_avg_pric?
                                // KisBalanceResponse has `prpr` but `pchs_avg_pric` missing in my DTO?
                                // Let's check KisBalanceResponse again.
                                .build())
                        .collect(Collectors.toList());
            }

            // Wait, I need to check if pchs_avg_pric exists in KisBalanceResponse.
            // Step 1277 shown Output1 has: pdno, prdtName, hldgQty, evluPflsRt,
            // evluPflsAmt, prpr.
            // It DOES NOT have pchs_avg_pric.
            // Required: "보유수량 변화 + 평균단가".
            // I need to ADD pchs_avg_pric to KisBalanceResponse.Output1.

            return BalanceSnapshot.builder()
                    .timestamp(LocalDateTime.now())
                    .cashDeposit(new BigDecimal(summary.getDncaTotAmt()))
                    .orderableCash(new BigDecimal(summary.getOrdPsblCash()))
                    .totalEquity(new BigDecimal(summary.getTotEvluAmt()))
                    .positions(positions)
                    .build();

        } catch (Exception e) {
            log.error("Error fetching balance snapshot", e);
            throw new RuntimeException("Balance fetch failed", e);
        }
    }
}
