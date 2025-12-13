package com.antigravity.trading.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "decision_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DecisionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String traceId; // Group related logs (Analysis -> Signal -> Trade)

    private Long backtestRunId; // Null for Live Trading

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private LocalDateTime eventTime;

    @Column(nullable = false)
    private String decisionType; // ENTRY_SIGNAL, EXIT_SIGNAL, HOLD, SKIP

    @Column(columnDefinition = "TEXT")
    private String inputsJson; // Snapshot of MarketEvent/Context (Price, MA20, etc.)

    @Column(columnDefinition = "TEXT")
    private String reasonsJson; // e.g. ["MA20 Pass", "Volume Fail", "Spread Pass"]

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null)
            this.createdAt = LocalDateTime.now();
    }
}
