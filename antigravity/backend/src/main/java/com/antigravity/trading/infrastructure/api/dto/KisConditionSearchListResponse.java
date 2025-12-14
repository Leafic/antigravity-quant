package com.antigravity.trading.infrastructure.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 한국투자증권 조건검색 목록 조회 응답 DTO
 * TR_ID: HHKST03900300
 */
@Data
public class KisConditionSearchListResponse {

    @JsonProperty("rt_cd")
    private String rtCd; // 응답코드

    @JsonProperty("msg_cd")
    private String msgCd; // 메시지코드

    @JsonProperty("msg1")
    private String msg1; // 메시지

    @JsonProperty("output2")
    private List<ConditionItem> output2;

    @Data
    public static class ConditionItem {
        @JsonProperty("user_id")
        private String userId; // 사용자 ID

        @JsonProperty("seq")
        private String seq; // 조건 시퀀스 번호

        @JsonProperty("condition_name")
        private String conditionName; // 조건명

        @JsonProperty("reg_date")
        private String regDate; // 등록일자
    }
}
