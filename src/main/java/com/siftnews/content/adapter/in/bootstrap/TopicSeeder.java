package com.siftnews.content.adapter.in.bootstrap;

import com.siftnews.content.application.port.in.SeedTopicsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 기동 이벤트를 받아 MVP 토픽(dev/ai/econ)을 심는 <b>인바운드 어댑터</b> — slug 기준 멱등.
 * <p>
 * 배치 Job과 마찬가지로 기동이라는 외부 자극을 유스케이스로 옮길 뿐이라 저장 방법(JPA)은 알지
 * 않는다. 심을 카탈로그는 {@link TopicSeedData}가 들고 있고, 저장은 {@link SeedTopicsUseCase}
 * → {@code SaveTopicPort} → persistence 어댑터로 흐른다 — Source 쪽 {@code SourceSeeder}와 같은
 * 구조다. 이전에는 out 어댑터에 놓인 채 JPA 리포지토리를 직접 부르고 엔티티 매핑까지 들고
 * 있었다(이슈 #39).
 * <p>
 * {@code test} 프로파일에서는 동작하지 않는다({@code @Profile("!test")}) — 통합 테스트는
 * 자기 데이터를 직접 준비한다(CLAUDE.md). Liquibase 마이그레이션 전환은 후속.
 * <p>
 * 시드는 배치 Job의 전제이므로 다른 러너보다 먼저 실행한다 — 근거는 {@code SourceSeeder} javadoc.
 */
@Component
@Profile("!test")
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
class TopicSeeder implements ApplicationRunner {

    private final SeedTopicsUseCase seedTopicsUseCase;

    @Override
    public void run(ApplicationArguments args) {
        seedTopicsUseCase.seed(TopicSeedData.topics());
    }
}
