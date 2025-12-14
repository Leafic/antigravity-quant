package com.antigravity.trading.infrastructure.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * KIS 종목 마스터 데이터 DTO
 * 한국투자증권에서 제공하는 종목 기본 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KisStockMasterDto {

    /**
     * 종목 코드 (6자리)
     */
    private String symbol;

    /**
     * 종목명 (한글)
     */
    private String name;

    /**
     * 시장 구분 (KOSPI, KOSDAQ, KONEX)
     */
    private String marketType;

    /**
     * 업종 (대분류)
     */
    private String sector;

    /**
     * 업종 (중분류)
     */
    private String industry;

    /**
     * 상장 주식 수
     */
    private Long listedShares;

    /**
     * 자본금 (백만원)
     */
    private Long capital;

    /**
     * 액면가
     */
    private Integer parValue;

    /**
     * 상장일 (YYYYMMDD)
     */
    private String listingDate;

    /**
     * 관리/주의 종목 여부
     */
    private Boolean isManaged;

    /**
     * 거래정지 여부
     */
    private Boolean isSuspended;
}
