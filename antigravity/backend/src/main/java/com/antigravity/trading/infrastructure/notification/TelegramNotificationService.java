package com.antigravity.trading.infrastructure.notification;

import com.antigravity.trading.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class TelegramNotificationService implements NotificationService {

    @Value("${telegram.bot-token}")
    private String botToken;

    @Value("${telegram.group-chat-id:}")
    private String groupChatId;

    @Value("${telegram.private-chat-ids:}")
    private String privateChatIdsRaw; // Comma separated

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void sendMessage(String message) {
        // Fallback for interface compatibility or legacy calls.
        // We warn because this shouldn't be used directly anymore for alerts.
        // Or we can route it to system alerts.
        sendSystemAlert(message);
    }

    @Override
    public void sendSignalNotification(String symbol, String type, String reason) {
        String emoji = type.equalsIgnoreCase("BUY") ? "📈" : "📉";
        String typeKr = type.equalsIgnoreCase("BUY") ? "매수 신호 (Signal)" : "매도 신호 (Signal)";

        String msg = String.format("""
                %s *%s 발생*

                🔍 종목: *%s*
                🛠 전략: *TrendMomentumV1*
                📝 사유: *%s*

                ------------------------
                ⚡ AntiGravity Strategy
                """, emoji, typeKr, symbol, reason);

        sendMessageToChat(groupChatId, msg);
    }

    @Override
    public void sendTradeNotification(String type, String symbol, String price, String quantity, String reason) {
        String emoji = type.equalsIgnoreCase("BUY") ? "🚀" : "👋";
        String typeKr = type.equalsIgnoreCase("BUY") ? "체결 (매수)" : "체결 (매도)";

        String msg = String.format("""
                %s *%s 완료*

                📋 종목: *%s*
                💰 가격: *%s KRW*
                🔢 수량: *%s주*
                📝 사유: *%s*

                ------------------------
                💳 계좌 잔고가 변동되었습니다.
                """, emoji, typeKr, symbol, price, quantity, reason);

        // Broadcast to all private users
        if (privateChatIdsRaw != null && !privateChatIdsRaw.isEmpty()) {
            String[] ids = privateChatIdsRaw.split(",");
            for (String id : ids) {
                sendMessageToChat(id.trim(), msg);
            }
        } else {
            log.warn("No private chat IDs configured for trade notification.");
        }
    }

    @Override
    public void sendSystemAlert(String message) {
        // System alerts go to private
        if (privateChatIdsRaw != null && !privateChatIdsRaw.isEmpty()) {
            String[] ids = privateChatIdsRaw.split(",");
            for (String id : ids) {
                sendMessageToChat(id.trim(), "🚨 *SYSTEM ALERT*\n" + message);
            }
        }
    }

    private void sendMessageToChat(String chatId, String text) {
        if (botToken == null || botToken.isEmpty() || chatId == null || chatId.isEmpty()) {
            log.warn("Telegram credentials (botToken or chatId) not configured or empty. Skipping message to {}",
                    chatId);
            return;
        }

        String url = String.format("https://api.telegram.org/bot%s/sendMessage", botToken);

        try {
            String payload = String.format("{\"chat_id\": \"%s\", \"text\": \"%s\", \"parse_mode\": \"Markdown\"}",
                    chatId, escapeJson(text));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(url, entity, String.class);
            log.debug("Telegram message sent to {}: {}", chatId, text);
        } catch (Exception e) {
            log.error("Failed to send Telegram message to {}: {}", chatId, e.getMessage());
        }
    }

    private String escapeJson(String text) {
        return text.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
