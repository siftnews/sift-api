package com.siftnews.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * {@code @Scheduled} 트리거 활성화.
 * <p>
 * {@code spring.batch.job.enabled=false}라 Job은 부팅 시 자동 실행되지 않는다 — 기동 수단은
 * 스케줄 트리거뿐이므로 이 설정이 없으면 배치가 영영 돌지 않는다.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
