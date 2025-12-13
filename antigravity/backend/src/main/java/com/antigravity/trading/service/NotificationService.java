package com.antigravity.trading.service;

public interface NotificationService {
    // Unified Engine Support
    void sendSignalNotification(com.antigravity.trading.engine.model.Signal signal);

    void sendTradeNotification(com.antigravity.trading.domain.entity.TradeLog trade,
            com.antigravity.trading.engine.model.BalanceSnapshot before,
            com.antigravity.trading.engine.model.BalanceSnapshot after);

    // Legacy Support (String-based)
    void sendMessage(String message);

    void sendSystemAlert(String message);

    void sendSignalNotification(String symbol, String type, String reason);

    void sendTradeNotification(String type, String symbol, String price, String quantity, String reason);
}
