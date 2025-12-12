package com.antigravity.trading.infrastructure.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class KisChartResponse {

    @JsonProperty("msg1")
    private String msg1;

    @JsonProperty("output1")
    private Output1 output1;

    @JsonProperty("output2")
    private List<Output2> output2;

    @Data
    public static class Output1 {
        @JsonProperty("prdy_vrss")
        private String prdyVrss; // 전일대비
        @JsonProperty("prdy_vrss_sign")
        private String prdyVrssSign; // 전일대비부호
        @JsonProperty("prdy_ctrt")
        private String prdyCtrt; // 전일대비율
    }

    @Data
    public static class Output2 {
        @JsonProperty("stck_bsop_date")
        private String stckBsopDate; // 영업일자 (YYYYMMDD)

        @JsonProperty("stck_oprc")
        private String stckOprc; // 시가

        @JsonProperty("stck_hgpr")
        private String stckHgpr; // 고가

        @JsonProperty("stck_lwpr")
        private String stckLwpr; // 저가

        @JsonProperty("stck_clpr")
        private String stckClpr; // 종가

        @JsonProperty("acml_vol")
        private String acmlVol; // 누적거래량
    }
}
