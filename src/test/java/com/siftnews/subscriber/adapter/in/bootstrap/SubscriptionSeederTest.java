package com.siftnews.subscriber.adapter.in.bootstrap;

import com.siftnews.content.api.TopicCatalog;
import com.siftnews.content.api.TopicReference;
import com.siftnews.subscriber.application.port.in.SeedSubscriptionsUseCase;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionSeederTest {

    @Test
    void resolvesTopicBySlugAndSeedsConfiguredEmailDomain() throws Exception {
        var fake = new RecordingSeedSubscriptionsUseCase();
        var topicCatalog = new FakeTopicCatalog(Optional.of(new TopicReference(42L)));

        new SubscriptionSeeder(fake, topicCatalog, "runner-test.sift.local", "dev").run(null);

        assertThat(fake.topicId).isEqualTo(42L);
        assertThat(fake.emailDomain).isEqualTo("runner-test.sift.local");
    }

    @Test
    void failsFastWhenConfiguredTopicIsMissing() {
        var fake = new RecordingSeedSubscriptionsUseCase();
        var topicCatalog = new FakeTopicCatalog(Optional.empty());

        assertThatThrownBy(() -> new SubscriptionSeeder(fake, topicCatalog,
                "runner-test.sift.local", "missing").run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing");
    }

    private record FakeTopicCatalog(Optional<TopicReference> topic) implements TopicCatalog {

        @Override
        public Optional<TopicReference> findActive(Long topicId) {
            return topic;
        }

        @Override
        public Optional<TopicReference> findActiveBySlug(String slug) {
            return topic;
        }
    }

    private static final class RecordingSeedSubscriptionsUseCase implements SeedSubscriptionsUseCase {

        private Long topicId;
        private String emailDomain;

        @Override
        public int seed(Long topicId, String emailDomain) {
            this.topicId = topicId;
            this.emailDomain = emailDomain;
            return 0;
        }
    }
}
