package com.antigravity.trading.infrastructure.persistence;

import com.antigravity.trading.infrastructure.api.KisApiClient;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultDataMigration implements CommandLineRunner {

    private final KisApiClient kisApiClient;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        String currentAccount = kisApiClient.getAccountNo();
        if (currentAccount == null || currentAccount.isEmpty()) {
            log.warn("Current Account No is unknown (env: kis.account-no is empty). Skipping data migration.");
            return;
        }

        log.info("Starting Data Migration for Account: {}", currentAccount);

        // Migrate TradeLog
        int tradeLogUpdated = entityManager.createQuery("UPDATE TradeLog t SET t.accountNo = :accountNo WHERE t.accountNo IS NULL")
                .setParameter("accountNo", currentAccount)
                .executeUpdate();

        if (tradeLogUpdated > 0) {
            log.info("Migrated {} TradeLog records to account '{}'", tradeLogUpdated, currentAccount);
        }

        // Migrate AutoTradeSchedule
        int scheduleUpdated = entityManager.createQuery("UPDATE AutoTradeSchedule s SET s.accountNo = :accountNo WHERE s.accountNo IS NULL")
                .setParameter("accountNo", currentAccount)
                .executeUpdate();

        if (scheduleUpdated > 0) {
            log.info("Migrated {} AutoTradeSchedule records to account '{}'", scheduleUpdated, currentAccount);
        }
    }
}
