package com.antigravity.trading.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 매매 로그 엔티티.
 * 매수/매도 주문 및 체결 내역을 기록합니다.
 */
@Entity
@Table(name = "trade_logs", indexes = {
        @Index(name = "idx_trade_symbol", columnList = "symbol"),
        @Index(name = "idx_trade_time", columnList = "timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class TradeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Trace ID for distributed tracing (MDC)
     */
    private String traceId;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeType type; // BUY, SELL

    @Column(length = 50)
    private String signalReason; // e.g. BREAKOUT_VOL

    @Column(precision = 19, scale = 4)
    private BigDecimal pnlPct; // Realized PnL %

    @Column(precision = 19, scale = 4)
    private BigDecimal price;

    private Integer quantity;

    @Column(length = 50)
    private String strategyName;

    @Column(length = 255)
    private String reason; // 매매 사유

    @Column(length = 50)
    private String signalCode; // BREAKOUT_MA20_VOL, etc.

    // Snapshot of indicators at entry
    @Column(precision = 19, scale = 4)
    private BigDecimal ma20;

    @Column(precision = 19, scale = 4)
    private BigDecimal rsi14;

    @Column(precision = 19, scale = 4)
    private BigDecimal volRatio;

    // Performance metrics (updated later)
    @Column(precision = 19, scale = 4)
    private BigDecimal mfe; // Max Favorable Excursion

    @Column(precision = 19, scale = 4)
    private BigDecimal mae; // Max Adverse Excursion

    @CreatedDate
    private LocalDateTime timestamp;

    public enum TradeType {
        BUY, SELL
    }
}
