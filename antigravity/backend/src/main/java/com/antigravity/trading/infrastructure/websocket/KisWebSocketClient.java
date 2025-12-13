package com.antigravity.trading.infrastructure.websocket;

import com.antigravity.trading.service.RealTimeTrader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.TextMessage;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisWebSocketClient extends TextWebSocketHandler {

    @Value("${kis.ws-url:ws://ops.koreainvestment.com:21000}")
    private String wsUrl;

    @Value("${kis.appkey}")
    private String appKey;

    @Value("${kis.appsecret}")
    private String appSecret;

    private final com.antigravity.trading.infrastructure.api.KisApiClient kisApiClient;

    private WebSocketSession session;
    private String approvalKey;

    @PostConstruct
    public void init() {
        // connect(); // Manually managed
    }

    public void connect() {
        WebSocketClient client = new StandardWebSocketClient();
        try {
            log.info("Fetching Approval Key...");
            this.approvalKey = kisApiClient.getApprovalKey();

            log.info("Connecting to KIS WebSocket: {}", wsUrl);
            client.doHandshake(this, wsUrl).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("WebSocket Connection Failed", e);
        }
    }

    public void subscribe(String symbol) {
        if (session == null || !session.isOpen()) {
            log.warn("WebSocket not connected. Cannot subscribe to {}", symbol);
            return;
        }

        if (approvalKey == null) {
            log.warn("Approval Key missing. Cannot subscribe.");
            return;
        }

        // KIS Subscription Json
        String request = String.format("""
                {
                    "header": {
                        "approval_key": "%s",
                        "custtype": "P",
                        "tr_type": "1",
                        "content-type": "utf-8"
                    },
                    "body": {
                        "input": {
                            "tr_id": "H0STCNT0",
                            "tr_key": "%s"
                        }
                    }
                }
                """, approvalKey, symbol);

        try {
            session.sendMessage(new TextMessage(request));
            log.info("Subscribed to {}", symbol);
        } catch (Exception e) {
            log.error("Subscription failed", e);
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket Connected: {}", session.getId());
        this.session = session;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        log.debug("WS Received: {}", payload);

        // Parse Tick (RealTimeTrader logic trigger)
        // if (payload.startsWith("0") || payload.startsWith("1")) { // Real data
        // realTimeTrader.onTick(payload);
        // }
    }
}
