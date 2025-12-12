package com.antigravity.trading.controller;

import com.antigravity.trading.service.DataArchiverService;
import com.antigravity.trading.service.RealTimeTrader;
import com.antigravity.trading.service.UniverseScreenerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final UniverseScreenerService screenerService;
    private final DataArchiverService archiverService;
    private final RealTimeTrader realTimeTrader;

    @PostMapping("/screen")
    public ResponseEntity<String> triggerScreening() {
        screenerService.executeScreening();
        return ResponseEntity.ok("Universe Screening Triggered (Phase 1)");
    }

    @PostMapping("/archive")
    public ResponseEntity<String> triggerArchiving() {
        archiverService.runDailyArchiving();
        return ResponseEntity.ok("Data Archiving Triggered (Phase 2)");
    }

    @PostMapping("/reload-targets")
    public ResponseEntity<String> reloadTargets() {
        realTimeTrader.loadActiveTargets();
        return ResponseEntity.ok("Active Targets Reloaded (Phase 3)");
    }
}
