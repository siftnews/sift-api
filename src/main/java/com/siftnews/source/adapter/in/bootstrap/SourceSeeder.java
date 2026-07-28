package com.siftnews.source.adapter.in.bootstrap;

import com.siftnews.source.application.port.in.SeedSourcesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 기동 이벤트를 받아 MVP 수집 소스 9종을 심는 <b>인바운드 어댑터</b> — url 기준 멱등.
 * <p>
 * 배치 Job과 마찬가지로 기동이라는 외부 자극을 유스케이스로 옮길 뿐이라 저장 방법(JPA)은 알지
 * 않는다. 심을 카탈로그는 {@link SourceSeedData}가 들고 있고, 저장은
 * {@link SeedSourcesUseCase} → {@code SaveSourcePort} → persistence 어댑터로 흐른다.
 * <p>
 * {@code test} 프로파일에서는 동작하지 않는다({@code @Profile("!test")}) — 통합 테스트는
 * 자기 데이터를 직접 준비한다(CLAUDE.md). Liquibase 마이그레이션 전환은 후속(MVP-DESIGN §2).
 * <p>
 * {@link Order}가 필요한 이유: Spring Batch의 {@code JobLauncherApplicationRunner}는 order가
 * {@code 0}인데 {@code @Order} 없는 러너는 {@link Ordered#LOWEST_PRECEDENCE}(맨 끝)라,
 * {@code spring.batch.job.enabled=true}로 기동하면 <b>Job이 시드보다 먼저 돌아</b> 소스 0건으로
 * 수집한다. 시드는 다른 러너의 전제이므로 가장 먼저 실행한다.
 */
@Component
@Profile("!test")
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
class SourceSeeder implements ApplicationRunner {

    private final SeedSourcesUseCase seedSourcesUseCase;

    @Override
    public void run(ApplicationArguments args) {
        seedSourcesUseCase.seed(SourceSeedData.sources());
    }
}
