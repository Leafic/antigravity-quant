package com.antigravity.trading.controller;

import com.antigravity.trading.service.KillSwitchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final KillSwitchService killSwitchService;

    @GetMapping("/kill-switch")
    public ResponseEntity<Boolean> getKillSwitchStatus() {
        return ResponseEntity.ok(killSwitchService.isSystemActive());
    }

    @PostMapping("/kill-switch")
    public ResponseEntity<String> setKillSwitch(@RequestParam boolean active) {
        killSwitchService.setSystemActive(active);
        String status = active ? "ACTIVATED (Trading Resumed)" : "DEACTIVATED (Trading Halted)";
        return ResponseEntity.ok("System Kill Switch " + status);
    }
}
