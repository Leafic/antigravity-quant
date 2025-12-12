package com.antigravity.trading.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 분봉 캔들 히스토리 엔티티.
 * Phase 2 (Data Archiving) 단계에서 수집된 1분봉 데이터 및 보조지표를 저장합니다.
 */
@Entity
@Table(name = "candle_history", indexes = {
        @Index(name = "idx_candle_symbol_time", columnList = "symbol, time", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandleHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String symbol;

    /**
     * 캔들 시작 시간
     */
    @Column(nullable = false)
    private LocalDateTime time;

    @Column(precision = 19, scale = 4)
    private BigDecimal open;

    @Column(precision = 19, scale = 4)
    private BigDecimal high;

    @Column(precision = 19, scale = 4)
    private BigDecimal low;

    @Column(precision = 19, scale = 4)
    private BigDecimal close;

    private Long volume;

    /**
     * 20일 이동평균선 (Pre-calculated)
     */
    @Column(name = "ma_20", precision = 19, scale = 4)
    private BigDecimal ma20;
    
    /**
     * 60일 이동평균선 (Pre-calculated)
     */
    @Column(name = "ma_60", precision = 19, scale = 4)
    private BigDecimal ma60;
}
