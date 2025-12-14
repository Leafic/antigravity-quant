package com.antigravity.trading;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AntiGravity 트레이딩 시스템 메인 애플리케이션 클래스.
 * 스케줄링 기능을 활성화(@EnableScheduling)하여 야간 배치 작업을 수행할 수 있도록 합니다.
 */
@SpringBootApplication
@EnableScheduling
public class TradingApplication {

    @jakarta.annotation.PostConstruct
    public void init() {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Seoul"));
    }

    public static void main(String[] args) {
        SpringApplication.run(TradingApplication.class, args);
    }
}
