package com.antigravity.trading.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 스케줄링 대상 종목 관리
 */
@Entity
@Table(name = "scheduled_stocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduledStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String symbol; // 종목코드

    @Column(length = 100)
    private String name; // 종목명

    @Column(nullable = false)
    private Boolean enabled; // 활성화 여부

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @Column(length = 500)
    private String note; // 메모 (선택 이유 등)
}
