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

    // Legacy / Interface Compatibility
    @Override
    public void sendSignalNotification(String symbol, String type, String reason) {
        // Adapt string params to new object if possible, or use simplified logic
        com.antigravity.trading.engine.model.Signal signal = com.antigravity.trading.engine.model.Signal.builder()
                .symbol(symbol)
                .type("BUY".equalsIgnoreCase(type) ? com.antigravity.trading.engine.model.Signal.Type.BUY
                        : com.antigravity.trading.engine.model.Signal.Type.SELL)
                .strategyName("Legacy/Unknown")
                .reasonCode("UNKNOWN")
                .reasonDetail(reason)
                .build();
        sendSignalNotification(signal);
    }

    @Override
    public void sendTradeNotification(String type, String symbol, String price, String quantity, String reason) {
        // Cannot create full snapshot here. Send simplified message or log warning.
        // We will just fetch CURRENT snapshot for both before/after (imprecise but
        // prevents crash)
        // Or just format a simple message using broadcastPrivate.
        String emoji = "BUY".equalsIgnoreCase(type) ? "🚀" : "👋";
        String msg = String.format("%s [LEGACY] %s %s %s주 @ %s\n%s", emoji, type, symbol, quantity, price, reason);
        broadcastPrivate(msg);
    }

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

    // Enhanced Methods
    public void sendSignalNotification(com.antigravity.trading.engine.model.Signal signal) {
        String emoji = signal.getType() == com.antigravity.trading.engine.model.Signal.Type.BUY ? "📈" : "📉";
        String typeKr = signal.getType() == com.antigravity.trading.engine.model.Signal.Type.BUY ? "매수 신호" : "매도 신호";

        String msg = String.format("""
                %s *%s 발생*

                🔍 종목: *%s*
                🛠 전략: *%s*
                📝 사유: *%s* (%s)

                ------------------------
                ⚡ AntiGravity Strategy
                """,
                emoji, typeKr, signal.getSymbol(),
                signal.getStrategyName(),
                signal.getReasonCode(), signal.getReasonDetail());

        sendMessageToChat(groupChatId, msg);
    }

    public void sendTradeNotification(com.antigravity.trading.domain.entity.TradeLog trade,
            com.antigravity.trading.engine.model.BalanceSnapshot before,
            com.antigravity.trading.engine.model.BalanceSnapshot after) {

        String side = trade.getType().name();
        String emoji = "BUY".equalsIgnoreCase(side) ? "🚀" : "👋";
        String title = "BUY".equalsIgnoreCase(side) ? "[매수] " + trade.getStrategyName()
                : "[매도] " + trade.getStrategyName();

        String body = "BUY".equalsIgnoreCase(side)
                ? buildBuyMessage(trade, before, after)
                : buildSellMessage(trade, before, after);

        String fullMsg = String.format("%s %s\n%s\ntraceId=%s", emoji, title, body, trade.getTraceId());

        broadcastPrivate(fullMsg);
    }

    private String buildBuyMessage(com.antigravity.trading.domain.entity.TradeLog trade,
            com.antigravity.trading.engine.model.BalanceSnapshot before,
            com.antigravity.trading.engine.model.BalanceSnapshot after) {
        return String.format("""
                종목: %s(%s) %s주 @ %s
                사유: %s

                주문가능금액: %s → %s (%s)
                총평가자산: %s → %s (%s, %s)
                예수금: %s → %s (%s)
                """,
                "UNKNOWN", trade.getSymbol(), trade.getQuantity(), formatMoney(trade.getPrice()),
                trade.getSignalReason(),

                formatMoney(before.getOrderableCash()), formatMoney(after.getOrderableCash()),
                formatDiffMoney(after.getOrderableCash().subtract(before.getOrderableCash())),
                formatMoney(before.getTotalEquity()), formatMoney(after.getTotalEquity()),
                formatDiffPercent(before.getTotalEquity(), after.getTotalEquity()),
                formatDiffMoney(after.getTotalEquity().subtract(before.getTotalEquity())),
                formatMoney(before.getCashDeposit()), formatMoney(after.getCashDeposit()),
                formatDiffMoney(after.getCashDeposit().subtract(before.getCashDeposit())));
    }

    private String buildSellMessage(com.antigravity.trading.domain.entity.TradeLog trade,
            com.antigravity.trading.engine.model.BalanceSnapshot before,
            com.antigravity.trading.engine.model.BalanceSnapshot after) {
        // Similar structure but add PnL if available in TradeLog
        return String.format("""
                종목: %s(%s) %s주 @ %s
                사유: %s
                손익: %s

                주문가능금액: %s → %s (%s)
                총평가자산: %s → %s (%s, %s)
                예수금: %s → %s (%s)
                """,
                "UNKNOWN", trade.getSymbol(), trade.getQuantity(), formatMoney(trade.getPrice()),
                trade.getSignalReason(),
                formatPercent(trade.getPnlPct()),

                formatMoney(before.getOrderableCash()), formatMoney(after.getOrderableCash()),
                formatDiffMoney(after.getOrderableCash().subtract(before.getOrderableCash())),
                formatMoney(before.getTotalEquity()), formatMoney(after.getTotalEquity()),
                formatDiffPercent(before.getTotalEquity(), after.getTotalEquity()),
                formatDiffMoney(after.getTotalEquity().subtract(before.getTotalEquity())),
                formatMoney(before.getCashDeposit()), formatMoney(after.getCashDeposit()),
                formatDiffMoney(after.getCashDeposit().subtract(before.getCashDeposit())));
    }

    private void broadcastPrivate(String msg) {
        if (privateChatIdsRaw != null && !privateChatIdsRaw.isEmpty()) {
            String[] ids = privateChatIdsRaw.split(",");
            for (String id : ids) {
                sendMessageToChat(id.trim(), msg);
            }
        }
    }

    // Formatters
    private String formatMoney(java.math.BigDecimal amount) {
        if (amount == null)
            return "0 KRW";
        return String.format("%,d KRW", amount.longValue());
    }

    private String formatDiffMoney(java.math.BigDecimal diff) {
        if (diff == null)
            return "0 KRW";
        long val = diff.longValue();
        return (val > 0 ? "+" : "") + String.format("%,d KRW", val);
    }

    private String formatPercent(java.math.BigDecimal pct) { // e.g. 0.012 -> 1.20%
        if (pct == null)
            return "0.00%";
        return String.format("%.2f%%", pct.multiply(new java.math.BigDecimal("100")));
    }

    private String formatDiffPercent(java.math.BigDecimal before, java.math.BigDecimal after) {
        if (before == null || before.compareTo(java.math.BigDecimal.ZERO) == 0)
            return "0.00%";
        java.math.BigDecimal diff = after.subtract(before).divide(before, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new java.math.BigDecimal("100"));
        return (diff.compareTo(java.math.BigDecimal.ZERO) > 0 ? "+" : "") + String.format("%.2f%%", diff);
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
