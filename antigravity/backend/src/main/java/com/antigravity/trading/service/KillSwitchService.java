package com.antigravity.trading.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Kill Switch Service
 * 시스템의 전역 안전 장치를 관리합니다.
 * 1. Redis를 이용한 글로벌 킬 스위치 (ON/OFF)
 * 2. 일일 손실 한도 체크 (Daily Loss Limit)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KillSwitchService {

    private final StringRedisTemplate redisTemplate;
    private final NotificationService notificationService;

    private static final String KILL_SWITCH_KEY = "system:kill-switch:active";
    private static final BigDecimal MAX_LOSS_PERCENT = new BigDecimal("-0.05"); // -5%

    /**
     * 킬 스위치가 활성화(정상 작동) 상태인지 확인
     * 
     * @return true if system is allowed to trade, false otherwise
     */
    public boolean isSystemActive() {
        String value = redisTemplate.opsForValue().get(KILL_SWITCH_KEY);
        // 키가 없으면 기본값은 false (안전제일) -> 아니면 배포 초기엔 true?
        // 요구사항: "Switch is off, system should not start".
        // 일단 기본값 true로 하되 Redis에 명시적으로 false가 있으면 중단.
        return !"false".equals(value);
    }

    /**
     * 킬 스위치 상태 변경
     */
    public void setSystemActive(boolean active) {
        redisTemplate.opsForValue().set(KILL_SWITCH_KEY, String.valueOf(active));
        String status = active ? "ACTIVE (Trading Resumed)" : "INACTIVE (Trading Halted)";
        log.warn("⚠️ Global Kill Switch status changed to: {}", status);
        notificationService.sendSystemAlert("Global Kill Switch status changed to: " + status);
    }

    /**
     * 일일 수익률 체크 및 손실 한도 초과 시 킬 스위치 발동
     */
    public void checkDailyLossLimit(BigDecimal dailyProfitLossPercent) {
        if (dailyProfitLossPercent.compareTo(MAX_LOSS_PERCENT) < 0) {
            log.error("🚨 Daily Loss Limit Triggered! P/L: {}%", dailyProfitLossPercent);
            setSystemActive(false); // Kill Switch Trigger
            notificationService.sendSystemAlert("🚨 Daily Loss Limit Triggered! System Halted via Kill Switch.");
        }
    }
}
