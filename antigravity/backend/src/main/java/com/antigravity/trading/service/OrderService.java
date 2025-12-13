package com.antigravity.trading.service;

import com.antigravity.trading.domain.entity.TradeLog;
import com.antigravity.trading.repository.TradeLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final com.antigravity.trading.infrastructure.api.KisApiClient kisApiClient;
    private final TradeLogRepository tradeLogRepository;
    private final NotificationService notificationService;

    public void placeBuyOrder(String symbol, BigDecimal price, int quantity, String strategy, String reason) {
        try {
            // Place Order
            String result = kisApiClient.placeOrder(symbol, "BUY", price.toString(), quantity);

            // Log Trade
            TradeLog tradeLog = TradeLog.builder()
                    .symbol(symbol)
                    .type(TradeLog.TradeType.BUY)
                    .price(price)
                    .quantity(quantity)
                    .strategyName(strategy)
                    .reason(reason)
                    .signalCode(reason) // simplified mapping
                    .timestamp(LocalDateTime.now())
                    .build();
            tradeLogRepository.save(tradeLog);

            // Notify
            notificationService.sendTradeNotification("BUY", symbol, price.toString(), String.valueOf(quantity),
                    reason);
            log.info("Buy Order Executed: {} {} @ {}", symbol, quantity, price);

        } catch (Exception e) {
            log.error("Failed to place BUY order for {}", symbol, e);
            notificationService.sendMessage("🚨 Order Failed: " + symbol + " - " + e.getMessage());
        }
    }
}
