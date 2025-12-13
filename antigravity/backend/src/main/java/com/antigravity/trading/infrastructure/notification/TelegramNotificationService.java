package com.antigravity.trading.infrastructure.notification;

import com.antigravity.trading.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class TelegramNotificationService implements NotificationService {

    @Value("${telegram.bot-token:}")
    private String botToken;

    @Value("${telegram.chat-id:}")
    private String chatId;

    private final WebClient webClient;

    public TelegramNotificationService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.telegram.org")
                .build();
    }

    @Override
    public void sendMessage(String message) {
        if (botToken == null || botToken.isEmpty() || chatId == null || chatId.isEmpty()) {
            log.warn("Telegram credentials not configured. Skipping message: {}", message);
            return;
        }

        try {
            webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/bot" + botToken + "/sendMessage")
                            .queryParam("chat_id", chatId)
                            .queryParam("text", message)
                            .queryParam("parse_mode", "Markdown")
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .subscribe(
                            response -> log.debug("Telegram sent: {}", message),
                            error -> log.error("Failed to send Telegram message", error));
        } catch (Exception e) {
            log.error("Telegram Error", e);
        }
    }

    @Override
    public void sendSystemAlert(String message) {
        sendMessage("🚨 SYSTEM ALERT: " + message);
    }

    @Override
    public void sendTradeNotification(String type, String symbol, String price, String quantity, String reason) {
        String msg = String.format("""
                🚀 *Trade Executed* (%s)
                Symbol: %s
                Price: %s
                Qty: %s
                Reason: %s
                ------------------------
                AntiGravity System
                """, type, symbol, price, quantity, reason);
        sendMessage(msg);
    }
}
