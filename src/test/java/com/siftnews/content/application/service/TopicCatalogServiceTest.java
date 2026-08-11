package com.siftnews.content.application.service;

import com.siftnews.content.api.TopicReference;
import com.siftnews.content.application.port.out.LoadTopicPort;
import com.siftnews.content.domain.Topic;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TopicCatalogServiceTest {

    @Test
    void resolvesActiveTopicBySlugForLoadtestWorkload() {
        Topic topic = topic(42L, true);
        TopicCatalogService service = new TopicCatalogService(
                new FakeLoadTopicPort(), slug -> Optional.of(topic));

        Optional<TopicReference> result = service.findActiveBySlug("dev");

        assertThat(result).contains(new TopicReference(42L));
    }

    @Test
    void doesNotExposeInactiveTopicBySlug() {
        Topic topic = topic(42L, false);
        TopicCatalogService service = new TopicCatalogService(
                new FakeLoadTopicPort(), slug -> Optional.of(topic));

        assertThat(service.findActiveBySlug("dev")).isEmpty();
    }

    private static Topic topic(Long topicId, boolean active) {
        Topic created = Topic.create("개발", "dev", "ko,en", List.of("Java"), List.of(),
                Map.of(), List.of("dev"), 24, 10, 0.0, active);
        return Topic.restore(topicId, created.getName(), created.getSlug(), created.getLangScope(),
                created.getIncludeKeywords(), created.getExcludeKeywords(), created.getKeywordWeights(),
                created.getSourceCategories(), created.getRecencyHalfLifeHours(), created.getMaxItems(),
                created.getScoreThreshold(), created.isActive());
    }

    private record FakeLoadTopicPort() implements LoadTopicPort {

        @Override
        public Optional<Topic> load(Long topicId) {
            return Optional.empty();
        }

        @Override
        public List<Topic> loadActive() {
            return List.of();
        }
    }
}
