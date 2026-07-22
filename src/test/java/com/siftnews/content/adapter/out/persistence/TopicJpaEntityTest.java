package com.siftnews.content.adapter.out.persistence;

import com.siftnews.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class TopicJpaEntityTest extends AbstractIntegrationTest {

    @Autowired
    private TopicJpaRepository topicJpaRepository;

    @Test
    void jsonColumnsRoundTrip() {
        TopicJpaEntity saved = topicJpaRepository.save(new TopicJpaEntity(
                "개발/엔지니어링", "dev", "ko,en",
                List.of("Spring", "Java"), List.of("광고"),
                Map.of("Spring", 2.0), List.of("dev", "programming"),
                24, 10, 0.5, true));

        TopicJpaEntity reloaded = topicJpaRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getIncludeKeywords()).containsExactly("Spring", "Java");
        assertThat(reloaded.getExcludeKeywords()).containsExactly("광고");
        assertThat(reloaded.getKeywordWeights()).containsEntry("Spring", 2.0);
        assertThat(reloaded.getSourceCategories()).containsExactly("dev", "programming");
        assertThat(reloaded.getLangScope()).isEqualTo("ko,en");
        assertThat(reloaded.getScoreThreshold()).isEqualTo(0.5);
        assertThat(reloaded.isActive()).isTrue();
    }

    @Test
    void slugIsUnique() {
        topicJpaRepository.save(new TopicJpaEntity(
                "AI/머신러닝", "ai", "ko,en",
                List.of("LLM"), List.of(), Map.of(), List.of("ai"),
                24, 10, 0.0, true));

        assertThatThrownBy(() -> topicJpaRepository.saveAndFlush(new TopicJpaEntity(
                "다른 AI", "ai", "en",
                List.of(), List.of(), Map.of(), List.of(),
                24, 10, 0.0, true)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
