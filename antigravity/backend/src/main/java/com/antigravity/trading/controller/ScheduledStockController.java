package com.antigravity.trading.controller;

import com.antigravity.trading.domain.entity.ScheduledStock;
import com.antigravity.trading.repository.ScheduledStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 스케줄링 대상 종목 관리 API
 */
@Slf4j
@RestController
@RequestMapping("/api/scheduled-stocks")
@RequiredArgsConstructor
public class ScheduledStockController {

    private final ScheduledStockRepository scheduledStockRepository;

    /**
     * 모든 스케줄링 종목 조회
     * GET /api/scheduled-stocks
     */
    @GetMapping
    public ResponseEntity<List<ScheduledStock>> getAllScheduledStocks() {
        List<ScheduledStock> stocks = scheduledStockRepository.findAll();
        return ResponseEntity.ok(stocks);
    }

    /**
     * 활성화된 스케줄링 종목만 조회
     * GET /api/scheduled-stocks/active
     */
    @GetMapping("/active")
    public ResponseEntity<List<ScheduledStock>> getActiveScheduledStocks() {
        List<ScheduledStock> stocks = scheduledStockRepository.findByEnabledTrue();
        return ResponseEntity.ok(stocks);
    }

    /**
     * 스케줄링 종목 추가
     * POST /api/scheduled-stocks
     * Body: { "symbol": "005930", "name": "삼성전자", "enabled": true, "note": "우량주" }
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> addScheduledStock(@RequestBody ScheduledStockRequest request) {
        log.info("Adding scheduled stock: {} ({})", request.getName(), request.getSymbol());

        // 중복 확인
        if (scheduledStockRepository.existsBySymbol(request.getSymbol())) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "이미 등록된 종목입니다: " + request.getSymbol());
            return ResponseEntity.badRequest().body(response);
        }

        ScheduledStock stock = ScheduledStock.builder()
                .symbol(request.getSymbol())
                .name(request.getName())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .note(request.getNote())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        scheduledStockRepository.save(stock);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "종목이 스케줄링 목록에 추가되었습니다");
        response.put("stock", stock);

        return ResponseEntity.ok(response);
    }

    /**
     * 스케줄링 종목 수정
     * PUT /api/scheduled-stocks/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateScheduledStock(
            @PathVariable Long id,
            @RequestBody ScheduledStockRequest request) {

        log.info("Updating scheduled stock ID: {}", id);

        return scheduledStockRepository.findById(id)
                .map(stock -> {
                    if (request.getName() != null) stock.setName(request.getName());
                    if (request.getEnabled() != null) stock.setEnabled(request.getEnabled());
                    if (request.getNote() != null) stock.setNote(request.getNote());
                    stock.setUpdatedAt(LocalDateTime.now());

                    scheduledStockRepository.save(stock);

                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "종목 정보가 수정되었습니다");
                    response.put("stock", stock);

                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "종목을 찾을 수 없습니다");
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * 스케줄링 종목 활성화/비활성화 토글
     * POST /api/scheduled-stocks/{id}/toggle
     */
    @PostMapping("/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleScheduledStock(@PathVariable Long id) {
        log.info("Toggling scheduled stock ID: {}", id);

        return scheduledStockRepository.findById(id)
                .map(stock -> {
                    stock.setEnabled(!stock.getEnabled());
                    stock.setUpdatedAt(LocalDateTime.now());
                    scheduledStockRepository.save(stock);

                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", stock.getEnabled() ? "종목이 활성화되었습니다" : "종목이 비활성화되었습니다");
                    response.put("enabled", stock.getEnabled());

                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "종목을 찾을 수 없습니다");
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * 스케줄링 종목 삭제
     * DELETE /api/scheduled-stocks/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteScheduledStock(@PathVariable Long id) {
        log.info("Deleting scheduled stock ID: {}", id);

        if (!scheduledStockRepository.existsById(id)) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "종목을 찾을 수 없습니다");
            return ResponseEntity.notFound().build();
        }

        scheduledStockRepository.deleteById(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "종목이 삭제되었습니다");

        return ResponseEntity.ok(response);
    }

    /**
     * 요청 DTO
     */
    public static class ScheduledStockRequest {
        private String symbol;
        private String name;
        private Boolean enabled;
        private String note;

        // Getters and Setters
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }

        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }
}
