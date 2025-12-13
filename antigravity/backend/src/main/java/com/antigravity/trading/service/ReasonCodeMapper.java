package com.antigravity.trading.service;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ReasonCodeMapper {

    private final Map<String, String> templates = new HashMap<>();

    public ReasonCodeMapper() {
        templates.put("BREAKOUT_VOL", "전일 고가 돌파 + 거래량 급증으로 추세 매수");
        templates.put("RSI_OVERSOLD_BUY", "RSI 과매도 구간 진입 후 반등 확인으로 매수 ({details})");
        templates.put("RSI_OVERBOUGHT_SELL", "RSI 과매수 구간 도달로 청산 ({details})");
        templates.put("SR_BREAKOUT_BUY", "저항선 돌파(최근 N봉 최고가 돌파)로 매수");
        templates.put("SR_BREAKDOWN_SELL", "지지선 붕괴(최근 N봉 최저가 이탈)로 매도");
        templates.put("ENSEMBLE_BUY", "복합 점수(확률) 기준 충족으로 매수 (confidence={conf})");
        templates.put("ENSEMBLE_SELL", "복합 점수(확률) 기준 충족으로 매도 (confidence={conf})");
        templates.put("STOP_LOSS", "손절 조건 충족 ({details})");
        templates.put("TAKE_PROFIT", "목표 수익률 도달로 익절");
        templates.put("TRAIL_STOP", "고점 대비 되밀림 발생으로 트레일링 스탑 청산");
        templates.put("REJECT_TIME", "진입 가능 시간 아님");
        templates.put("REJECT_VOLUME", "거래량 부족");
        templates.put("REJECT_MA20_TREND", "역추세(MA20 하회)로 진입 보류");
        templates.put("REJECT_PRICE_BELOW_TARGET", "목표가(Breakout) 미달성");
    }

    public String mapToKorean(String code, String details, Double confidence) {
        String template = templates.getOrDefault(code, code);
        if (details != null) {
            template = template.replace("{details}", details);
        }
        if (confidence != null) {
            template = template.replace("{conf}", String.format("%.2f", confidence));
        }
        return template;
    }
}
