package com.siftnews.content.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TopicTest {

    @Test
    void createNormalizesSlugToLowercase() {
        Topic topic = Topic.create("개발/엔지니어링", "  DEV  ", "ko,en",
                List.of("Spring", "Java"), List.of("광고"),
                Map.of("Spring", 2.0), List.of("dev", "programming"),
                24, 10, 0.5, true);

        assertThat(topic.getSlug()).isEqualTo("dev");
    }

    @Test
    void createMapsAllFields() {
        Topic topic = Topic.create("AI/머신러닝", "ai", "ko,en",
                List.of("LLM"), List.of(), Map.of("LLM", 3.0), List.of("ai", "ml"),
                12, 8, 0.3, true);

        assertThat(topic.getTopicId()).isNull();
        assertThat(topic.getName()).isEqualTo("AI/머신러닝");
        assertThat(topic.getLangScope()).isEqualTo("ko,en");
        assertThat(topic.getIncludeKeywords()).containsExactly("LLM");
        assertThat(topic.getKeywordWeights()).containsEntry("LLM", 3.0);
        assertThat(topic.getSourceCategories()).containsExactly("ai", "ml");
        assertThat(topic.getMaxItems()).isEqualTo(8);
        assertThat(topic.isActive()).isTrue();
    }

    @Test
    void createDefaultsNullCollectionsToEmpty() {
        Topic topic = Topic.create("경제", "econ", "ko,en",
                null, null, null, null, 24, 10, 0.0, true);

        assertThat(topic.getIncludeKeywords()).isEmpty();
        assertThat(topic.getExcludeKeywords()).isEmpty();
        assertThat(topic.getKeywordWeights()).isEmpty();
        assertThat(topic.getSourceCategories()).isEmpty();
    }

    @Test
    void createRejectsBlankSlug() {
        assertThatThrownBy(() -> Topic.create("이름", "  ", "ko",
                null, null, null, null, 24, 10, 0.0, true))
                .isInstanceOf(TopicException.class);
    }

    @Test
    void createRejectsSlugWithIllegalCharacters() {
        assertThatThrownBy(() -> Topic.create("이름", "개발", "ko",
                null, null, null, null, 24, 10, 0.0, true))
                .isInstanceOf(TopicException.class);
    }

    @Test
    void createRejectsBlankName() {
        assertThatThrownBy(() -> Topic.create(" ", "dev", "ko",
                null, null, null, null, 24, 10, 0.0, true))
                .isInstanceOf(TopicException.class);
    }

    @Test
    void createRejectsNonPositiveRecencyAndMaxItems() {
        assertThatThrownBy(() -> Topic.create("이름", "dev", "ko",
                null, null, null, null, 0, 10, 0.0, true))
                .isInstanceOf(TopicException.class);
        assertThatThrownBy(() -> Topic.create("이름", "dev", "ko",
                null, null, null, null, 24, 0, 0.0, true))
                .isInstanceOf(TopicException.class);
    }

    @Test
    void createRejectsNegativeScoreThreshold() {
        assertThatThrownBy(() -> Topic.create("이름", "dev", "ko",
                null, null, null, null, 24, 10, -0.1, true))
                .isInstanceOf(TopicException.class);
    }
}
