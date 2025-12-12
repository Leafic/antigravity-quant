package com.antigravity.trading.infrastructure.api;

import com.antigravity.trading.infrastructure.api.dto.KisBalanceResponse;
import com.antigravity.trading.infrastructure.api.dto.KisTokenResponse;
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
     * 주식 잔고 조회 (예수금/평가금)
     */
    public Map<String, BigDecimal> getAccountBalance() {
        String token = getAccessToken();
        String cano = accountNo.substring(0, 8);
        String prdt = "01"; // Default product code

        log.debug("Fetching account balance for {}-{}", cano, prdt);

        KisBalanceResponse response = webClient.get()
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

        Map<String, BigDecimal> result = new HashMap<>();
        if (response != null && "0".equals(response.getRtCd()) && response.getOutput2() != null
                && !response.getOutput2().isEmpty()) {
            KisBalanceResponse.Output2 output = response.getOutput2().get(0);
            result.put("totalEvaluation", new BigDecimal(output.getTotEvluAmt()));
            result.put("deposit", new BigDecimal(output.getDncaTotAmt()));
        } else {
            // Fallback or Error
            log.error("Failed to fetch balance: {}", response != null ? response.getMsg1() : "No Response");
            result.put("totalEvaluation", BigDecimal.ZERO);
            result.put("deposit", BigDecimal.ZERO);
        }
        return result;
    }

    /**
     * 현재가 조회 (Mock 유지 - 실시간 시세는 웹소켓이 유리함)
     */
    public BigDecimal getCurrentPrice(String symbol) {
        // ... (이전과 동일, 필요시 구현)
        return new BigDecimal("70000");
    }

    /**
     * 1분봉 조회 (Mock 유지 - 데이터 수집용)
     */
    public void getMinuteCandles(String symbol, LocalDateTime from, LocalDateTime to) {
        // ... (이전과 동일)
    }
}
