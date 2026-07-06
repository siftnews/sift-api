package com.siftnews.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * 시간 소스를 빈으로 제공한다.
 * <p>
 * 도메인/서비스는 {@code Instant.now()} 대신 주입받은 {@link Clock}을 사용해
 * 시각을 얻는다 → 테스트에서 고정 시계로 대체 가능(결정론적 검증).
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
