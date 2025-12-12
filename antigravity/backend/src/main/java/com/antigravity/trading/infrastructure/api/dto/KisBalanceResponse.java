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

    @JsonProperty("output2")
    private List<Output2> output2;

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
    }
}
