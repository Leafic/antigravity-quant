package com.antigravity.trading.controller;

import com.antigravity.trading.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/test")
    public ResponseEntity<String> sendTestMessage(@RequestBody TestMessageRequest request) {
        if ("TRADE".equalsIgnoreCase(request.type)) {
            String symbol = request.symbol != null ? request.symbol : "005930";
            String reason = request.reason != null ? request.reason : "테스트 매수 체결";

            notificationService.sendTradeNotification(
                    "BUY", symbol, "34,500", "10", reason);
            return ResponseEntity.ok("Trade test message sent (Private).");
        } else if ("SIGNAL".equalsIgnoreCase(request.type)) {
            String symbol = request.symbol != null ? request.symbol : "005930";
            String reason = request.reason != null ? request.reason : "테스트 매수 신호";

            notificationService.sendSignalNotification(symbol, "BUY", reason);
            return ResponseEntity.ok("Signal test message sent (Group).");
        } else {
            notificationService.sendSystemAlert(request.message != null ? request.message : "Test System Alert");
            return ResponseEntity.ok("System alert sent.");
        }
    }

    public record TestMessageRequest(String type, String message, String symbol, String reason) {
    }
}
