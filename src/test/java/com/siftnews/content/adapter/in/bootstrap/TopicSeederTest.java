package com.siftnews.content.adapter.in.bootstrap;

import com.siftnews.content.application.port.in.SeedTopicsUseCase;
import com.siftnews.content.application.port.out.LoadTopicPort;
import com.siftnews.content.domain.Topic;
import com.siftnews.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 시더는 {@code @Profile("!test")}라 테스트 컨텍스트에 빈으로 뜨지 않으므로,
 * 직접 생성해 Testcontainers DB에 대해 시드·멱등을 검증한다.
 */
@Transactional
class TopicSeederTest extends AbstractIntegrationTest {

    @Autowired
    private SeedTopicsUseCase seedTopicsUseCase;

    @Autowired
    private LoadTopicPort loadTopicPort;

    @Test
    void seedsThreeTopicsIdempotently() {
        runSeeder();

        assertThat(loadTopicPort.loadActive())
                .hasSize(3)
                .extracting(Topic::getSlug)
                .containsExactlyInAnyOrder("dev", "ai", "econ");

        runSeeder();

        assertThat(loadTopicPort.loadActive()).hasSize(3);
    }

    /**
     * 재기동은 <b>예외 없이 0건</b>이어야 한다 — 저장이 conflict-ignore가 아니면 slug 유니크 제약에
     * 걸려 두 번째 기동이 죽는다(두 인스턴스가 동시에 기동할 때 한쪽이 죽는 것과 같은 경로).
     */
    @Test
    void reseedInsertsNothing() {
        List<Topic> topics = TopicSeedData.topics();

        assertThat(seedTopicsUseCase.seed(topics)).isEqualTo(3);
        assertThat(seedTopicsUseCase.seed(topics)).isZero();
    }

    /**
     * JSON 컬럼 4종이 네이티브 insert를 거쳐도 온전히 왕복하는지 — 직렬화를 손으로 하는 경로라
     * 여기가 깨지면 선별이 빈 키워드로 돌아 후보를 못 고른다.
     */
    @Test
    void seedRoundTripsJsonColumns() {
        runSeeder();

        Topic dev = loadTopicPort.loadActive().stream()
                .filter(topic -> topic.getSlug().equals("dev"))
                .findFirst()
                .orElseThrow();

        assertThat(dev.getName()).isEqualTo("개발/엔지니어링");
        assertThat(dev.getLangScope()).isEqualTo("ko,en");
        assertThat(dev.getIncludeKeywords()).contains("Spring", "Kubernetes", "백엔드");
        assertThat(dev.getExcludeKeywords()).isEmpty();
        assertThat(dev.getKeywordWeights()).isEmpty();
        assertThat(dev.getSourceCategories()).containsExactlyInAnyOrder("dev", "programming");
        assertThat(dev.getRecencyHalfLifeHours()).isEqualTo(24);
        assertThat(dev.getMaxItems()).isEqualTo(10);
        assertThat(dev.getScoreThreshold()).isZero();
        assertThat(dev.isActive()).isTrue();
    }

    private void runSeeder() {
        new TopicSeeder(seedTopicsUseCase).run(null);
    }
}
