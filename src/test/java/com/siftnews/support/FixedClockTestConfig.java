package com.siftnews.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/** 통합 테스트의 벽시계 의존을 제거하는 고정 시간 소스. */
@TestConfiguration(proxyBeanMethods = false)
public class FixedClockTestConfig {

    private static final Instant FIXED_NOW = Instant.parse("2026-08-05T21:00:00Z");

    @Bean
    @Primary
    Clock fixedClock() {
        return Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    }
}
