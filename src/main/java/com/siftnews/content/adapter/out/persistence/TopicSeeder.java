package com.siftnews.content.adapter.out.persistence;

import com.siftnews.content.domain.Topic;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 기동 시 MVP 토픽(dev/ai/econ)을 시드한다 — slug 기준 멱등(이미 있으면 건너뜀).
 * <p>
 * {@code test} 프로파일에서는 동작하지 않는다({@code @Profile("!test")}) — 통합 테스트는
 * 자기 데이터를 직접 준비한다(CLAUDE.md). Liquibase 마이그레이션 전환은 후속(MVP-DESIGN §2).
 * <p>
 * 시드는 배치 Job의 전제이므로 다른 러너보다 먼저 실행한다 — 근거는 {@code SourceSeeder} javadoc.
 */
@Component
@Profile("!test")
@Order(Ordered.HIGHEST_PRECEDENCE)
class TopicSeeder implements ApplicationRunner {

    private final TopicJpaRepository topicJpaRepository;

    TopicSeeder(TopicJpaRepository topicJpaRepository) {
        this.topicJpaRepository = topicJpaRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (Topic topic : TopicSeedData.topics()) {
            if (!topicJpaRepository.existsBySlug(topic.getSlug())) {
                topicJpaRepository.save(toEntity(topic));
            }
        }
    }

    private static TopicJpaEntity toEntity(Topic topic) {
        return new TopicJpaEntity(topic.getName(), topic.getSlug(), topic.getLangScope(),
                topic.getIncludeKeywords(), topic.getExcludeKeywords(), topic.getKeywordWeights(),
                topic.getSourceCategories(), topic.getRecencyHalfLifeHours(), topic.getMaxItems(),
                topic.getScoreThreshold(), topic.isActive());
    }
}
