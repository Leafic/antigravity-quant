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
            notificationService.sendTradeNotification(
                    "BUY", "005930", "70000", "10", "TEST_SIGNAL");
            return ResponseEntity.ok("Trade test message sent.");
        } else {
            notificationService.sendSystemAlert(request.message != null ? request.message : "Test System Alert");
            return ResponseEntity.ok("System alert sent.");
        }
    }

    public record TestMessageRequest(String type, String message) {
    }
}
