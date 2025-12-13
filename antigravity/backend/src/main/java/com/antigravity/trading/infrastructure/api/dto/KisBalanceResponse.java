package com.antigravity.trading.infrastructure.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class KisBalanceResponse {
    @JsonProperty("rt_cd")
    private String rtCd;

    @JsonProperty("msg1")
    private String msg1;

    @JsonProperty("output1")
    private List<Output1> output1;

    @JsonProperty("output2")
    private List<Output2> output2;

    @Data
    public static class Output1 {
        @JsonProperty("pdno")
        private String pdno; // 종목번호

        @JsonProperty("prdt_name")
        private String prdtName; // 상품명

        @JsonProperty("hldg_qty")
        private String hldgQty; // 보유수량

        @JsonProperty("evlu_pfls_rt")
        private String evluPflsRt; // 평가손익율

        @JsonProperty("evlu_pfls_amt")
        private String evluPflsAmt; // 평가손익금액

        @JsonProperty("prpr")
        private String prpr; // 현재가

        @JsonProperty("pchs_avg_pric")
        private String pchsAvgPric; // 매입평균가격
    }

    @Data
    public static class Output2 {
        @JsonProperty("dnca_tot_amt")
        private String dncaTotAmt; // 예수금총액

        @JsonProperty("nxdy_excc_amt")
        private String nxdyExccAmt; // D+1 정산금

        @JsonProperty("prvs_rcdl_excc_amt")
        private String prvsRcdlExccAmt; // 가수도정산금

        @JsonProperty("tot_evlu_amt")
        private String totEvluAmt; // 총평가금액

        @JsonProperty("ord_psbl_cash")
        private String ordPsblCash; // 주문가능금액
    }
}
