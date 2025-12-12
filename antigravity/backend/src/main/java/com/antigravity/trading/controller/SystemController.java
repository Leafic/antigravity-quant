package com.antigravity.trading.controller;

import com.antigravity.trading.service.KillSwitchService;
import com.antigravity.trading.service.NotificationService;
import com.antigravity.trading.service.RiskManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final KillSwitchService killSwitchService;
    private final NotificationService notificationService;
    private final RiskManagementService riskManagementService;

    @GetMapping("/kill-switch")
    public ResponseEntity<Boolean> getKillSwitchStatus() {
        return ResponseEntity.ok(killSwitchService.isSystemActive());
    }

    @PostMapping("/kill-switch")
    public ResponseEntity<String> toggleKillSwitch(@RequestParam boolean active) {
        String msg = killSwitchService.setActive(active);
        notificationService
                .sendMessage("⚠️ System Kill Switch: " + (active ? "ON (Trading Stopped)" : "OFF (Trading Resumed)"));
        return ResponseEntity.ok(msg);
    }

    @PostMapping("/risk/daily-loss-limit")
    public ResponseEntity<String> setDailyLossLimit(@RequestParam java.math.BigDecimal limit) {
        riskManagementService.setDailyLossLimit(limit);
        return ResponseEntity.ok("Daily Loss Limit set to " + limit + "%");
    }
}
