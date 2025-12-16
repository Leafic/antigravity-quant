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

    public void placeOrder(String symbol, String type, BigDecimal price, int quantity, String strategy, String reason) {
        try {
            // Place Order
            String result = kisApiClient.placeOrder(symbol, type, price.toString(), quantity);

            // Log Trade
            TradeLog tradeLog = TradeLog.builder()
                    .symbol(symbol)
                    .type(TradeLog.TradeType.valueOf(type))
                    .price(price)
                    .quantity(quantity)
                    .strategyName(strategy)
                    .reason(reason)
                    .signalCode(reason)
                    .timestamp(LocalDateTime.now())
                    .build();
            tradeLogRepository.save(tradeLog);

            // Notify
            String priceStr;
            if (price.compareTo(BigDecimal.ZERO) == 0) {
                BigDecimal estimatedPrice = kisApiClient.getCurrentPrice(symbol);
                if (estimatedPrice.compareTo(BigDecimal.ZERO) > 0) {
                    priceStr = "Market Price (~" + estimatedPrice + ")";
                } else {
                    priceStr = "Market Price";
                }
            } else {
                priceStr = price.toString();
            }
            notificationService.sendTradeNotification(type, symbol, priceStr, String.valueOf(quantity), reason);
            log.info("{} Order Executed: {} {} @ {}", type, symbol, quantity, price);

        } catch (Exception e) {
            log.error("Failed to place {} order for {}", type, symbol, e);
            notificationService.sendMessage("🚨 Order Failed: " + symbol + " - " + e.getMessage());
            throw new RuntimeException("Order placement failed", e); // Re-throw for Controller to catch if needed
        }
    }

    public void placeBuyOrder(String symbol, BigDecimal price, int quantity, String strategy, String reason) {
        placeOrder(symbol, "BUY", price, quantity, strategy, reason);
    }
}
