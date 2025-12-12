package com.antigravity.trading.service;

import com.antigravity.trading.domain.entity.TradeLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Notification Service
 * 시스템의 중요 이벤트(매매, 에러, 킬 스위치 등)를 외부 메신저로 전송합니다.
 * 현재는 로그로 대체하며, 추후 Slack/Telegram Webhook으로 확장 가능합니다.
 */
@Slf4j
@Service
public class NotificationService {

    public void sendTradeAlert(TradeLog tradeLog) {
        // TODO: Slack Webhook Integration
        String message = String.format("🚨 [TRADE EXECUTION] %s %s %d shares @ %s",
                tradeLog.getType(),
                tradeLog.getSymbol(),
                tradeLog.getQuantity(),
                tradeLog.getPrice());

        log.info("Sending Notification: {}", message);
    }

    public void sendErrorAlert(String errorMessage) {
        // TODO: Slack Webhook Integration
        String message = String.format("❌ [SYSTEM ERROR] %s", errorMessage);
        log.error("Sending Notification: {}", message);
    }

    public void sendSystemAlert(String message) {
        log.info("📢 [SYSTEM ALERT] {}", message);
    }
}
