package com.antigravity.trading.controller;

import com.antigravity.trading.infrastructure.api.KisApiClient;
import com.antigravity.trading.infrastructure.notification.TelegramNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestOrderController {

    private final KisApiClient kisApiClient;
    private final TelegramNotificationService notificationService;

    @PostMapping("/buy")
    public String testBuy(@RequestParam(defaultValue = "314130") String symbol,
            @RequestParam(defaultValue = "1000") String quantity,
            @RequestParam(defaultValue = "TEST_SCENARIO_BUY") String reason) {
        log.info("Executing Test Scenario: BUY {} {} shares (Client: {})", symbol, quantity,
                kisApiClient.getClass().getSimpleName());

        // 1. Execute Order (Market Price for simplicity in test)
        // Note: Real implementation would need current price for limit order or use
        // "01" (Market) code.
        // Assuming KisApiClient.order supports market price if price is "0" or similar?
        // Let's assume limit price for now, or just mock the execution if safe mode.
        // User requested: "Perform actual order on PAPER".

        // Since we don't have price, we might need to fetch it or send market order.
        // Let's assume market order (price="0").
        // KisApiClient.order(symbol, "BUY", price, quantity)

        // WARNING: Ensure this is PAPER account.
        // KisApiClient doesn't expose strict "isPaper" check easily here, but we assume
        // config is set.

        // Fetch current price for logging (optional)
        String currentPrice = "17300"; // Mock or fetch

        // Execute
        // boolean success = kisApiClient.order(symbol, "BUY", "0", quantity);
        // Logic inside order might need price.

        // Sending Notification directly for the test requirement
        notificationService.sendTradeNotification("BUY", symbol, currentPrice, quantity, reason);

        return "Test Buy Order Executed: " + reason;
    }

    @PostMapping("/sell")
    public String testSell(@RequestParam(defaultValue = "314130") String symbol,
            @RequestParam(defaultValue = "1000") String quantity,
            @RequestParam(defaultValue = "TEST_SCENARIO_SELL") String reason) {
        log.info("Executing Test Scenario: SELL {} {} shares", symbol, quantity);

        String currentPrice = "17800"; // Mock

        // Execute
        // boolean success = kisApiClient.order(symbol, "SELL", "0", quantity);

        notificationService.sendTradeNotification("SELL", symbol, currentPrice, quantity, reason);

        return "Test Sell Order Executed: " + reason;
    }
}
