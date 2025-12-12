package com.antigravity.trading.configuration;

import com.antigravity.trading.service.UniverseScreenerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupRunner implements CommandLineRunner {

    private final UniverseScreenerService screenerService;

    @Override
    public void run(String... args) throws Exception {
        log.info("StartupRunner: Executing initial universe screening...");
        try {
            screenerService.executeScreening();
            log.info("StartupRunner: Screening completed successfully.");
        } catch (Exception e) {
            log.error("StartupRunner: Failed to execute screening", e);
        }
    }
}
