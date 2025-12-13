package com.antigravity.trading.service;

public interface NotificationService {
    // Legacy support
    void sendMessage(String message);

    void sendSystemAlert(String message);

    void sendSignalNotification(String symbol, String type, String reason);

    void sendTradeNotification(String type, String symbol, String price, String quantity, String reason);
}
