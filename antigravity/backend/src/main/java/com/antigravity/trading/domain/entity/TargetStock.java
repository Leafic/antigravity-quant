package com.antigravity.trading.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 모니터링 대상 종목 엔티티.
 * Phase 1 (Universe Screening) 단계에서 선정된 종목들을 저장합니다.
 */
@Entity
@Table(name = "target_stocks", indexes = {
        @Index(name = "idx_target_stock_symbol", columnList = "symbol", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class TargetStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 종목 코드 (예: 005930)
     */
    @Column(nullable = false, unique = true, length = 20)
    private String symbol;

    /**
     * 종목명 (예: 삼성전자)
     */
    @Column(nullable = false)
    private String name;

    /**
     * 시장 구분 (예: KOSPI, KOSDAQ)
     */
    @Column(length = 20)
    private String market;

    /**
     * 섹터/업종 (스크리닝 조건 확인용)
     */
    private String sector;

    /**
     * 선정된 날짜 (배치 실행일)
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 활성 상태 여부 (다음 날 배치가 돌기 전까지 유효)
     */
    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;
}
