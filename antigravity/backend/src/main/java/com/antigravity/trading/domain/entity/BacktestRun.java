package com.antigravity.trading.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "backtest_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    @Column(columnDefinition = "TEXT")
    private String paramsJson; // { "fromDate": "...", "strategy": "...", "capital": ... }

    private String status; // RUNNING, COMPLETED, FAILED

    @Column(columnDefinition = "TEXT")
    private String summaryJson; // { "totalPnl": ..., "winRate": ..., "mdd": ... }
}
