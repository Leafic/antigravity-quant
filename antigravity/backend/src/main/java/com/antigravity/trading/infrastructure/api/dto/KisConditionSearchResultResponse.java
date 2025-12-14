package com.antigravity.trading.infrastructure.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 한국투자증권 조건검색 종목 조회 응답 DTO
 * TR_ID: HHKST03900400
 */
@Data
public class KisConditionSearchResultResponse {

    @JsonProperty("rt_cd")
    private String rtCd; // 응답코드

    @JsonProperty("msg_cd")
    private String msgCd; // 메시지코드

    @JsonProperty("msg1")
    private String msg1; // 메시지

    @JsonProperty("output2")
    private List<StockItem> output2;

    @Data
    public static class StockItem {
        @JsonProperty("stock_code")
        private String stockCode; // 종목코드

        @JsonProperty("stock_name")
        private String stockName; // 종목명

        @JsonProperty("current_price")
        private String currentPrice; // 현재가

        @JsonProperty("change_rate")
        private String changeRate; // 등락률

        @JsonProperty("volume")
        private String volume; // 거래량

        @JsonProperty("market_cap")
        private String marketCap; // 시가총액
    }
}
