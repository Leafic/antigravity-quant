package com.antigravity.trading.infrastructure.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class KisMinuteChartResponse {

    @JsonProperty("output1")
    private Output1 output1;

    @JsonProperty("output2")
    private List<Output2> output2;

    @Data
    public static class Output1 {
        @JsonProperty("prdy_vrss")
        private String prdyVrss;
        @JsonProperty("prdy_vrss_sign")
        private String prdyVrssSign;
        @JsonProperty("prdy_ctrt")
        private String prdyCtrt;
    }

    @Data
    public static class Output2 {
        @JsonProperty("stck_bsop_time")
        private String stckBsopTime; // 체결시간 (HHMMSS)

        @JsonProperty("stck_prpr")
        private String stckPrpr; // 현재가(종가)

        @JsonProperty("stck_oprc")
        private String stckOprc; // 시가

        @JsonProperty("stck_hgpr")
        private String stckHgpr; // 고가

        @JsonProperty("stck_lwpr")
        private String stckLwpr; // 저가

        @JsonProperty("acml_vol")
        private String acmlVol; // 누적거래량
    }
}
