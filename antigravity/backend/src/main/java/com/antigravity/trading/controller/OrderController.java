package com.antigravity.trading.controller;

import com.antigravity.trading.service.OrderService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> placeOrder(@RequestBody OrderRequest request) {
        log.info("Manual Order Requested: {}", request);

        try {
            // Price "0" implies market price in our convention for now, or use provided price
            BigDecimal price = (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) == 0) 
                    ? BigDecimal.ZERO 
                    : request.getPrice();

            orderService.placeOrder(
                    request.getSymbol(),
                    request.getType(),
                    price,
                    request.getQuantity(),
                    "MANUAL",
                    request.getReason() != null ? request.getReason() : "Manual Order"
            );

            return ResponseEntity.ok(Map.of("success", true, "message", "Order placed successfully"));
        } catch (Exception e) {
            log.error("Manual order failed", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @Data
    public static class OrderRequest {
        private String symbol;
        private String type; // BUY, SELL
        private BigDecimal price;
        private int quantity;
        private String reason;
    }
}
