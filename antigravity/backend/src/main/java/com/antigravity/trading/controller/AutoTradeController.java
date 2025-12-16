package com.antigravity.trading.controller;

import com.antigravity.trading.domain.entity.AutoTradeSchedule;
import com.antigravity.trading.service.AutoTradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/autotrade")
@RequiredArgsConstructor
public class AutoTradeController {

    private final AutoTradeService service;

    @GetMapping
    public ResponseEntity<List<AutoTradeSchedule>> getAllSchedules() {
        return ResponseEntity.ok(service.getAllSchedules());
    }

    @PostMapping
    public ResponseEntity<AutoTradeSchedule> createSchedule(@RequestBody AutoTradeSchedule schedule) {
        return ResponseEntity.ok(service.createSchedule(schedule));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AutoTradeSchedule> updateSchedule(@PathVariable Long id, @RequestBody AutoTradeSchedule schedule) {
        return ResponseEntity.ok(service.updateSchedule(id, schedule));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        service.deleteSchedule(id);
        return ResponseEntity.ok().build();
    }
}
