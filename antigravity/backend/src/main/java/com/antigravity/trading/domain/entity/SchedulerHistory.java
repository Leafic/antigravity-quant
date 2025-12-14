package com.antigravity.trading.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 스케줄러 실행 히스토리
 */
@Entity
@Table(name = "scheduler_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchedulerHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String jobName; // "DATA_COLLECTION", "INDICATOR_CALCULATION"

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column
    private LocalDateTime endTime;

    @Column(nullable = false, length = 20)
    private String status; // "RUNNING", "SUCCESS", "FAILED"

    @Column(length = 1000)
    private String message; // 실행 결과 메시지

    @Column
    private Integer totalItems; // 처리된 종목 수 등

    @Column
    private Integer successItems;

    @Column
    private Integer failedItems;

    @Column(length = 5000)
    private String errorDetails; // 에러 상세 정보
}
