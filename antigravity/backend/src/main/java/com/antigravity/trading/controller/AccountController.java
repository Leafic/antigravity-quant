package com.antigravity.trading.controller;

import com.antigravity.trading.infrastructure.api.KisApiClient;
import com.antigravity.trading.infrastructure.api.dto.KisBalanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final KisApiClient kisApiClient;

    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> getBalance() {
        try {
            KisBalanceResponse response = kisApiClient.getAccountBalance();
            Map<String, Object> result = new HashMap<>();

            if (response != null && response.getOutput2() != null && !response.getOutput2().isEmpty()) {
                KisBalanceResponse.Output2 summary = response.getOutput2().get(0);
                result.put("totalEvaluation", summary.getTotEvluAmt());
                result.put("deposit", summary.getDncaTotAmt());
            } else {
                result.put("totalEvaluation", "0");
                result.put("deposit", "0");
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace(); // Log error for docker logs
            return ResponseEntity.ok(Map.of("totalEvaluation", "0", "deposit", "0"));
        }
    }

    @GetMapping("/holdings")
    public ResponseEntity<List<KisBalanceResponse.Output1>> getHoldings() {
        try {
            KisBalanceResponse response = kisApiClient.getAccountBalance();
            if (response != null && response.getOutput1() != null) {
                return ResponseEntity.ok(response.getOutput1());
            }
            return ResponseEntity.ok(Collections.emptyList());
        } catch (Exception e) {
            e.printStackTrace(); // Log error for docker logs
            return ResponseEntity.ok(Collections.emptyList());
        }
    }
}
