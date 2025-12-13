package com.antigravity.trading.engine.model;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class BalanceSnapshot {
    private final LocalDateTime timestamp;
    private final BigDecimal cashDeposit; // 예수금 (dnca_tot_amt)
    private final BigDecimal orderableCash; // 주문가능금액 (ord_psbl_cash)
    private final BigDecimal totalEquity; // 총평가자산 (tot_evlu_amt)

    private final List<PositionSnapshot> positions;

    @Getter
    @Builder
    public static class PositionSnapshot {
        private final String symbol;
        private final String name;
        private final long quantity;
        private final BigDecimal avgPrice;
    }
}
