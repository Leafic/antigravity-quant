package com.antigravity.trading.scheduler;

import com.antigravity.trading.infrastructure.api.KisApiClient;
import com.antigravity.trading.infrastructure.notification.TelegramNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GenomeAutoTradeScheduler {

    private final KisApiClient kisApiClient;
    private final TelegramNotificationService notificationService;

    // Genome & Company (314130)
    private static final String SYMBOL = "314130";
    private static final String QTY = "10"; // Default Qty (Adjust as needed)

    /**
     * Buy at Market Open (09:00 KST)
     * We trigger slightly after open to ensure market is active, or use Market
     * Order ("01")
     * Trigger at 09:00:05 KST
     */
    @Scheduled(cron = "5 0 9 * * MON-FRI", zone = "Asia/Seoul")
    public void buyAtOpen() {
        log.info("[Scheduler] Executing Buy for {} at Open", SYMBOL);
        try {
            // Market Order (Price "0")
            // Logic: kisApiClient.placeOrder(SYMBOL, "BUY", "0", Integer.parseInt(QTY));
            // Note: Ensure your KisApiClient supports "0" as market price or handles it.
            // If not, you might need to fetch current price + buffer.
            // Assuming "0" corresponds to Market Order in implementation logic or user
            // rules.

            String response = kisApiClient.placeOrder(SYMBOL, "BUY", "0", Integer.parseInt(QTY));
            boolean success = response != null && !response.isEmpty(); // Simple check

            if (success) {
                notificationService.sendTradeNotification("BUY", SYMBOL, "MARKET_OPEN", QTY, "Scheduled Open Buy");
            } else {
                notificationService.sendTradeNotification("ERROR", SYMBOL, "0", QTY,
                        "Scheduled Buy Failed (Empty Response)");
            }
        } catch (Exception e) {
            log.error("Failed scheduled buy", e);
            notificationService.sendTradeNotification("ERROR", SYMBOL, "0", QTY, "Exception: " + e.getMessage());
        }
    }

    /**
     * Sell at Market Close (15:20 KST)
     * Trigger at 15:19:00 KST just before close to liquidate
     */
    @Scheduled(cron = "0 19 15 * * MON-FRI", zone = "Asia/Seoul")
    public void sellAtClose() {
        log.info("[Scheduler] Executing Sell for {} at Close", SYMBOL);
        try {
            // Market Sell
            String response = kisApiClient.placeOrder(SYMBOL, "SELL", "0", Integer.parseInt(QTY));
            boolean success = response != null && !response.isEmpty();

            if (success) {
                notificationService.sendTradeNotification("SELL", SYMBOL, "MARKET_CLOSE", QTY, "Scheduled Close Sell");
            } else {
                notificationService.sendTradeNotification("ERROR", SYMBOL, "0", QTY, "Scheduled Sell Failed");
            }
        } catch (Exception e) {
            log.error("Failed scheduled sell", e);
            notificationService.sendTradeNotification("ERROR", SYMBOL, "0", QTY, "Exception: " + e.getMessage());
        }
    }
}
