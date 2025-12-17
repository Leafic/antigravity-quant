package com.antigravity.trading.infrastructure.api;

import com.antigravity.trading.infrastructure.api.dto.KisBalanceResponse;
import com.antigravity.trading.infrastructure.api.dto.KisTokenResponse;
import com.antigravity.trading.infrastructure.api.dto.KisChartResponse;
import com.antigravity.trading.infrastructure.api.dto.KisMinuteChartResponse;
import com.antigravity.trading.infrastructure.api.dto.KisConditionSearchListResponse;
import com.antigravity.trading.infrastructure.api.dto.KisConditionSearchResultResponse;
import com.antigravity.trading.infrastructure.api.dto.KisStockMasterDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.*;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

        try {
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
                .header("tr_id", isVirtual() ? "VTTC8434R" : "TTTC8434R")
                .retrieve()
                .bodyToMono(KisBalanceResponse.class)
                .block();
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            String errorBody = e.getResponseBodyAsString();
            log.error("KIS API Error ({}): {}", e.getStatusCode(), errorBody);
            throw new RuntimeException("KIS API Failed: " + errorBody, e);
        }
    }

    private boolean isVirtual() {
        return baseUrl != null && baseUrl.contains("vts");
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
        KisChartResponse finalResponse = new KisChartResponse();
        finalResponse.setOutput2(new java.util.ArrayList<>());

        LocalDateTime currentEnd = end;
        LocalDateTime currentStart = end.minusDays(30); // 30일 단위로 분할
        String targetStartStr = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(start);

        while (currentEnd.isAfter(start) || currentEnd.isEqual(start)) {
            // 시작일이 목표 시작일보다 이전이면 목표 시작일로 조정
            if (currentStart.isBefore(start)) {
                currentStart = start;
            }

            String startStr = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(currentStart);
            String endStr = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(currentEnd);

            log.debug("Fetching daily chart for {} from {} to {} (30-day batch)", symbol, startStr, endStr);

            try {
                String token = getAccessToken();
                KisChartResponse response = webClient.get()
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

                if (response != null && response.getOutput2() != null && !response.getOutput2().isEmpty()) {
                    // 응답 데이터 검증 로그
                    KisChartResponse.Output2 firstCandle = response.getOutput2().get(0);
                    log.info("KIS API Response for {} - First candle: date={}, close={}",
                        symbol, firstCandle.getStckBsopDate(), firstCandle.getStckClpr());

                    finalResponse.getOutput2().addAll(response.getOutput2());
                    if (finalResponse.getOutput1() == null)
                        finalResponse.setOutput1(response.getOutput1()); // Set generic info once
                }

                // API 호출 제한을 위한 딜레이
                Thread.sleep(200);

            } catch (Exception e) {
                log.warn("Failed to fetch chart data for {} ({} ~ {}): {}", symbol, startStr, endStr, e.getMessage());
                // 에러 발생 시 해당 구간은 건너뛰고 계속 진행
            }

            // 다음 30일 구간으로 이동
            if (currentStart.isEqual(start)) {
                break; // 시작일에 도달하면 종료
            }
            currentEnd = currentStart.minusDays(1);
            currentStart = currentEnd.minusDays(30);
        }

        // Remove duplicates and sort if needed (Data comes desc, we are appending
        // chunks, so it might be partially ordered)
        // KIS returns desc (recent first). Batch 1: recent..old. Batch 2:
        // older..oldest.
        // So simple append keeps desc order.

        return finalResponse;
    }

    /**
     * 현재가 조회 (Mock 유지 - 실시간 시세는 웹소켓이 유리함)
     */
    public BigDecimal getCurrentPrice(String symbol) {
        try {
            KisMinuteChartResponse response = getMinuteChart(symbol);
            if (response != null && response.getOutput2() != null && !response.getOutput2().isEmpty()) {
                String currentPriceStr = response.getOutput2().get(0).getStckPrpr();
                return new BigDecimal(currentPriceStr);
            }
        } catch (Exception e) {
            log.error("Failed to fetch current price for {}", symbol, e);
        }
        return BigDecimal.ZERO;
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
     * 조건검색 목록 조회
     * TR_ID: HHKST03900300
     */
    public KisConditionSearchListResponse getConditionSearchList(String userId) {
        String token = getAccessToken();

        log.debug("Fetching condition search list for user: {}", userId);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/psearch-title")
                        .queryParam("user_id", userId)
                        .queryParam("seq", "0")
                        .build())
                .header("authorization", "Bearer " + token)
                .header("appkey", appKey)
                .header("appsecret", appSecret)
                .header("tr_id", "HHKST03900300")
                .retrieve()
                .bodyToMono(KisConditionSearchListResponse.class)
                .block();
    }

    /**
     * 조건검색 종목 조회
     * TR_ID: HHKST03900400
     */
    public KisConditionSearchResultResponse getConditionSearchResult(String userId, String seq) {
        String token = getAccessToken();

        log.debug("Fetching condition search result for user: {}, seq: {}", userId, seq);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/psearch-result")
                        .queryParam("user_id", userId)
                        .queryParam("seq", seq)
                        .build())
                .header("authorization", "Bearer " + token)
                .header("appkey", appKey)
                .header("appsecret", appSecret)
                .header("tr_id", "HHKST03900400")
                .retrieve()
                .bodyToMono(KisConditionSearchResultResponse.class)
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
        String ordDvsn = "0".equals(price) ? "01" : "00"; // 00: Limit, 01: Market
        body.put("ORD_DVSN", ordDvsn);
        body.put("ORD_QTY", String.valueOf(quantity));
        body.put("ORD_UNPR", price);

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

    /**
     * KOSPI/KOSDAQ 종목 마스터 데이터 다운로드 및 파싱
     * 한국투자증권 다운로드 서버에서 마스터 파일(.mst.zip)을 다운로드하여 파싱합니다.
     *
     * @param marketType "KOSPI" or "KOSDAQ"
     * @return 종목 마스터 데이터 리스트
     */
    public List<KisStockMasterDto> downloadStockMasterData(String marketType) {
        String downloadUrl = getDownloadUrl(marketType);
        log.info("Downloading {} stock master data from: {}", marketType, downloadUrl);

        try {
            // 1. ZIP 파일 다운로드 및 압축 해제
            byte[] mstData = downloadAndExtractMst(downloadUrl);

            // 2. MST 파일 파싱 (CP949 인코딩)
            List<KisStockMasterDto> stockList = parseMstData(mstData, marketType);

            log.info("Successfully parsed {} {} stocks", stockList.size(), marketType);
            return stockList;

        } catch (Exception e) {
            log.error("Failed to download {} stock master data: {}", marketType, e.getMessage(), e);
            throw new RuntimeException("Failed to download stock master data for " + marketType, e);
        }
    }

    /**
     * 마스터 데이터 다운로드 URL 반환
     */
    private String getDownloadUrl(String marketType) {
        String baseUrl = "https://new.real.download.dws.co.kr/common/master";

        return switch (marketType.toUpperCase()) {
            case "KOSPI" -> baseUrl + "/kospi_code.mst.zip";
            case "KOSDAQ" -> baseUrl + "/kosdaq_code.mst.zip";
            case "KONEX" -> baseUrl + "/konex_code.mst.zip";
            default -> throw new IllegalArgumentException("Unsupported market type: " + marketType);
        };
    }

    /**
     * ZIP 파일 다운로드 및 압축 해제
     */
    private byte[] downloadAndExtractMst(String downloadUrl) throws IOException {
        URL url = new URL(downloadUrl);

        try (InputStream in = url.openStream();
             ZipInputStream zipIn = new ZipInputStream(in)) {

            ZipEntry entry = zipIn.getNextEntry();
            if (entry == null) {
                throw new IOException("No entry found in ZIP file");
            }

            log.debug("Extracting: {}", entry.getName());

            // ZIP 내부의 MST 파일 읽기
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[4096];
            int bytesRead;

            while ((bytesRead = zipIn.read(data)) != -1) {
                buffer.write(data, 0, bytesRead);
            }

            return buffer.toByteArray();
        }
    }

    /**
     * MST 파일 데이터 파싱
     * 고정폭 바이너리 파일 (CP949 인코딩)
     *
     * 파일 구조:
     * - 첫 9자리: 단축코드 (종목코드)
     * - 9~21자리: 표준코드
     * - 21자리 이후: 한글명 + 기타 데이터 (고정폭 필드)
     */
    private List<KisStockMasterDto> parseMstData(byte[] mstData, String marketType) throws IOException {
        List<KisStockMasterDto> result = new ArrayList<>();
        Charset cp949 = Charset.forName("MS949"); // CP949 = MS949 in Java

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(mstData), cp949))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.length() < 228) {
                    // 최소 길이 미만인 라인은 스킵
                    continue;
                }

                try {
                    KisStockMasterDto stock = parseMstLine(line, marketType);
                    if (stock != null) {
                        result.add(stock);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse line (skipping): {}", e.getMessage());
                }
            }
        }

        return result;
    }

    /**
     * MST 파일의 한 줄을 파싱하여 StockMasterDto로 변환
     *
     * 고정폭 필드 위치 (Python 코드 참조):
     * - 0~9: 단축코드 (종목코드)
     * - 9~21: 표준코드
     * - 21~61: 한글명
     * - 이후: 다양한 메타데이터 필드들
     */
    private KisStockMasterDto parseMstLine(String line, String marketType) {
        if (line.length() < 228) {
            return null;
        }

        try {
            // Part 1: 기본 정보
            String symbol = line.substring(0, 9).trim();          // 단축코드 (종목코드)
            String standardCode = line.substring(9, 21).trim();   // 표준코드
            String name = line.substring(21, 61).trim();          // 한글명

            // Part 2: 추가 메타데이터 (고정폭 필드 - 마지막 228자)
            // Python 코드의 필드 정의를 참조하여 주요 필드만 추출
            int offset = line.length() - 228;

            // 주요 필드 추출 (필요한 필드만 선택)
            String sector = extractField(line, offset + 0, 4).trim();      // 업종 대분류
            String listingDateStr = extractField(line, offset + 190, 8);    // 상장일 (YYYYMMDD)
            String managedFlag = extractField(line, offset + 80, 1);        // 관리종목 여부
            String suspendedFlag = extractField(line, offset + 81, 1);      // 거래정지 여부

            return KisStockMasterDto.builder()
                    .symbol(symbol)
                    .name(name)
                    .marketType(marketType)
                    .sector(sector)
                    .listingDate(listingDateStr.trim())
                    .isManaged("1".equals(managedFlag.trim()))
                    .isSuspended("1".equals(suspendedFlag.trim()))
                    .build();

        } catch (Exception e) {
            log.warn("Error parsing MST line: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 고정폭 필드 추출 헬퍼 메서드
     */
    private String extractField(String line, int start, int length) {
        int endIndex = Math.min(start + length, line.length());
        if (start >= line.length()) {
            return "";
        }
        return line.substring(start, endIndex);
    }
}
