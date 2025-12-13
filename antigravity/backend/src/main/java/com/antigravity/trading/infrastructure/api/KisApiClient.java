package com.antigravity.trading.infrastructure.api;

import com.antigravity.trading.infrastructure.api.dto.KisBalanceResponse;
import com.antigravity.trading.infrastructure.api.dto.KisTokenResponse;
import com.antigravity.trading.infrastructure.api.dto.KisChartResponse;
import com.antigravity.trading.infrastructure.api.dto.KisMinuteChartResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Korea Investment Securities (KIS) API Integration Client.
 * KIS REST API와 통신을 담당합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KisApiClient {

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    @Value("${kis.account-no}")
    private String accountNo;

    @Value("${kis.base-url:https://openapivts.koreainvestment.com:29443}")
    private String baseUrl;

    private final WebClient.Builder webClientBuilder;
    private WebClient webClient;
    private String accessToken;
    private LocalDateTime tokenExpiry;

    @PostConstruct
    public void init() {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        // 초기 토큰 발급 시도 (실패해도 앱 구동은 되도록 try-catch)
        try {
            getAccessToken();
        } catch (Exception e) {
            log.error("Failed to initialize KIS Access Token: {}", e.getMessage());
        }
    }

    /**
     * 접근 토큰 발급 (OAuth2)
     */
    public synchronized String getAccessToken() {
        if (accessToken != null && tokenExpiry != null && LocalDateTime.now().isBefore(tokenExpiry)) {
            return accessToken;
        }

        log.info("Requesting new KIS Access Token...");
        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "client_credentials");
        body.put("appkey", appKey);
        body.put("appsecret", appSecret);

        KisTokenResponse response = webClient.post()
                .uri("/oauth2/tokenP")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(KisTokenResponse.class)
                .block();

        if (response != null && response.getAccessToken() != null) {
            this.accessToken = response.getAccessToken();
            this.tokenExpiry = LocalDateTime.now().plusSeconds(response.getExpiresIn() - 60); // 1분 여유
            log.info("KIS Access Token acquired. Expires in {} seconds.", response.getExpiresIn());
            return accessToken;
        } else {
            throw new RuntimeException("Failed to get Access Token from KIS API");
        }
    }

    /**
     * WebSocket 접속용 Approval Key 발급
     */
    public String getApprovalKey() {
        log.info("Fetching WebSocket Approval Key...");
        // POST /oauth2/Approval
        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "client_credentials");
        body.put("appkey", appKey);
        body.put("secretkey", appSecret);

        try {
            String response = webClient.post()
                    .uri("/oauth2/Approval")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // Response: { "approval_key": "..." }
            if (response != null && response.contains("approval_key")) {
                return new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get("approval_key")
                        .asText();
            }
        } catch (Exception e) {
            log.error("Failed to get Approval Key", e);
        }
        return null;
    }

    /**
     * 주식 잔고 조회 (Full Response)
     */
    public KisBalanceResponse getAccountBalance() {
        String token = getAccessToken();
        String cano = accountNo.substring(0, 8);
        String prdt = "01"; // Default product code

        log.debug("Fetching account balance for {}-{}", cano, prdt);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/trading/inquire-balance")
                        .queryParam("CANO", cano)
                        .queryParam("ACNT_PRDT_CD", prdt)
                        .queryParam("AFHR_FLPR_YN", "N")
                        .queryParam("OFL_YN", "")
                        .queryParam("INQR_DVSN", "02")
                        .queryParam("UNPR_DVSN", "01")
                        .queryParam("FUND_STTL_ICLD_YN", "N")
                        .queryParam("FNCG_AMT_AUTO_RDPT_YN", "N")
                        .queryParam("PRCS_DVSN", "00")
                        .queryParam("CTX_AREA_FK100", "")
                        .queryParam("CTX_AREA_NK100", "")
                        .build())
                .header("authorization", "Bearer " + token)
                .header("appkey", appKey)
                .header("appsecret", appSecret)
                .header("tr_id", "VTTC8434R") // 실전: TTTC8434R, 모의: VTTC8434R
                .retrieve()
                .bodyToMono(KisBalanceResponse.class)
                .block();
    }

    /**
     * 일봉 차트 조회 (기간별)
     * KIS API: 국내주식기간별시세 (거래량 집계)
     * TR_ID: FHKST01010100
     */
    /**
     * 일봉 차트 조회 (기간별)
     * KIS API: 국내주식기간별시세
     */
    public KisChartResponse getDailyChart(String symbol) {
        // Default: Last 100 days
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(100);
        return getDailyChart(symbol, start, end);
    }

    public KisChartResponse getDailyChart(String symbol, LocalDateTime start, LocalDateTime end) {
        String token = getAccessToken();
        String startStr = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(start);
        String endStr = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(end);

        log.debug("Fetching daily chart for {} from {} to {}", symbol, startStr, endStr);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice")
                        .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                        .queryParam("FID_INPUT_ISCD", symbol)
                        .queryParam("FID_INPUT_DATE_1", startStr)
                        .queryParam("FID_INPUT_DATE_2", endStr)
                        .queryParam("FID_PERIOD_DIV_CODE", "D")
                        .queryParam("FID_ORG_ADJ_PRC", "1")
                        .build())
                .header("authorization", "Bearer " + token)
                .header("appkey", appKey)
                .header("appsecret", appSecret)
                .header("tr_id", "FHKST03010100")
                .retrieve()
                .bodyToMono(KisChartResponse.class)
                .block();
    }

    /**
     * 현재가 조회 (Mock 유지 - 실시간 시세는 웹소켓이 유리함)
     */
    public BigDecimal getCurrentPrice(String symbol) {
        // ... (이전과 동일, 필요시 구현)
        return new BigDecimal("70000");
    }

    /**
     * 1분봉 차트 조회 (실시간)
     * KIS API: 주식당일분봉조회
     * TR_ID: FHKST03010200
     */
    public KisMinuteChartResponse getMinuteChart(String symbol) {
        String token = getAccessToken();
        String time = java.time.format.DateTimeFormatter.ofPattern("HHmmss").format(LocalDateTime.now());

        log.debug("Fetching minute chart for {} at {}", symbol, time);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/inquire-time-itemchartprice")
                        .queryParam("FID_ETC_CLS_CODE", "")
                        .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                        .queryParam("FID_INPUT_ISCD", symbol)
                        .queryParam("FID_INPUT_HOUR_1", time) // 조회 기준 시간 (현재시간)
                        .queryParam("FID_PW_DATA_INCU_YN", "Y") // 과거 데이터 포함
                        .build())
                .header("authorization", "Bearer " + token)
                .header("appkey", appKey)
                .header("appsecret", appSecret)
                .header("tr_id", "FHKST03010200") // 주식당일분봉조회
                .retrieve()
                .bodyToMono(KisMinuteChartResponse.class)
                .block();
    }

    /**
     * 주식 주문 (매수/매도)
     * type: "BUY" or "SELL"
     */
    public String placeOrder(String symbol, String type, String price, Integer quantity) {
        String token = getAccessToken();
        String trId = "BUY".equals(type) ? "VTTC0802U" : "VTTC0801U"; // Simulation Buy/Sell. Real: TTTC0802U/TTTC0801U

        // TODO: Switch TR_ID based on config (Real vs Simulation). Hardcoded to Sim for
        // prototype phase.

        Map<String, String> body = new HashMap<>();
        body.put("CANO", accountNo.substring(0, 8));
        body.put("ACNT_PRDT_CD", "01");
        body.put("PDNO", symbol);
        body.put("ORD_DVSN", "00"); // 00: Limit (지정가), 01: Market (시장가)
        body.put("ORD_QTY", String.valueOf(quantity));
        body.put("ORD_UNPR", price); // Price (0 for Market)

        log.info("Placing {} Order for {} (Qty: {}, Price: {})", type, symbol, quantity, price);

        try {
            String response = webClient.post()
                    .uri("/uapi/domestic-stock/v1/trading/order-cash")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("authorization", "Bearer " + token)
                    .header("appkey", appKey)
                    .header("appsecret", appSecret)
                    .header("tr_id", trId)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return response;
        } catch (Exception e) {
            log.error("Order Failed", e);
            throw new RuntimeException("Order Execution Failed", e);
        }
    }
}
