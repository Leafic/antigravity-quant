package com.antigravity.trading.service;

public interface NotificationService {
    void sendMessage(String message);

    void sendTradeNotification(String type, String symbol, String price, String quantity, String reason);

    void sendSystemAlert(String message);
}
