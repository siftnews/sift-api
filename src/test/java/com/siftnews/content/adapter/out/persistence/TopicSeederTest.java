package com.siftnews.content.adapter.out.persistence;

import com.siftnews.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 시더는 {@code @Profile("!test")}라 테스트 컨텍스트에 빈으로 뜨지 않으므로,
 * 직접 생성해 Testcontainers DB에 대해 시드·멱등을 검증한다.
 */
@Transactional
class TopicSeederTest extends AbstractIntegrationTest {

    @Autowired
    private TopicJpaRepository topicJpaRepository;

    @Test
    void seedsThreeTopicsIdempotently() {
        TopicSeeder seeder = new TopicSeeder(topicJpaRepository);

        seeder.run(null);

        assertThat(topicJpaRepository.count()).isEqualTo(3);
        assertThat(topicJpaRepository.existsBySlug("dev")).isTrue();
        assertThat(topicJpaRepository.existsBySlug("ai")).isTrue();
        assertThat(topicJpaRepository.existsBySlug("econ")).isTrue();

        seeder.run(null);

        assertThat(topicJpaRepository.count()).isEqualTo(3);
    }
}
