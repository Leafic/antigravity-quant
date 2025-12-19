package com.antigravity.trading.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "auto_trade_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class AutoTradeSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String symbol; // 종목코드

    @Column(length = 100)
    private String name; // 종목명

    @Column(length = 20)
    private String accountNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeType type; // BUY, SELL

    @Column(nullable = false)
    private String scheduleTime; // HH:mm format (e.g., "09:00")

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Boolean isActive;

    @CreatedDate
    private LocalDateTime createdAt;

    private LocalDateTime lastExecutedAt;

    public enum TradeType {
        BUY, SELL
    }
}
