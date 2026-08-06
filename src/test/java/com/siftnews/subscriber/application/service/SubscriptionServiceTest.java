package com.siftnews.subscriber.application.service;

import com.siftnews.content.api.TopicCatalog;
import com.siftnews.content.api.TopicReference;
import com.siftnews.subscriber.application.port.out.LoadSubscriberPort;
import com.siftnews.subscriber.application.port.out.LoadSubscriptionPort;
import com.siftnews.subscriber.domain.ConflictException;
import com.siftnews.subscriber.domain.Subscriber;
import com.siftnews.subscriber.domain.SubscriberStatus;
import com.siftnews.subscriber.domain.Subscription;
import com.siftnews.subscriber.domain.SubscriptionStatus;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionServiceTest {

    private static final Subscriber SUBSCRIBER = Subscriber.restore(
            1L, "user@example.com", SubscriberStatus.ACTIVE, 8);

    @Test
    void subscribesToActiveTopic() {
        SubscriptionService service = new SubscriptionService(
                new FakeLoadSubscriberPort(),
                (subscriberId, topicId) -> Optional.empty(),
                subscription -> Subscription.restore(2L, subscription.getSubscriberId(),
                        subscription.getTopicId(), subscription.getStatus()),
                new FakeTopicCatalog());

        var result = service.subscribe(1L, 3L);

        assertThat(result.subscriptionId()).isEqualTo(2L);
        assertThat(result.status()).isEqualTo(SubscriptionStatus.ACTIVE.name());
    }

    @Test
    void unsubscribesExistingSubscription() {
        Subscription active = Subscription.restore(2L, 1L, 3L, SubscriptionStatus.ACTIVE);
        var saved = new Subscription[1];
        SubscriptionService service = new SubscriptionService(
                new FakeLoadSubscriberPort(),
                (subscriberId, topicId) -> Optional.of(active),
                subscription -> {
                    saved[0] = subscription;
                    return subscription;
                },
                new FakeTopicCatalog());

        service.unsubscribe(1L, 3L);

        assertThat(saved[0].getStatus()).isEqualTo(SubscriptionStatus.PAUSED);
    }

    @Test
    void reactivatesPausedSubscription() {
        Subscription paused = Subscription.restore(2L, 1L, 3L, SubscriptionStatus.PAUSED);
        var saved = new Subscription[1];
        SubscriptionService service = new SubscriptionService(
                new FakeLoadSubscriberPort(),
                (subscriberId, topicId) -> Optional.of(paused),
                subscription -> {
                    saved[0] = subscription;
                    return subscription;
                },
                new FakeTopicCatalog());

        var result = service.subscribe(1L, 3L);

        assertThat(result.status()).isEqualTo(SubscriptionStatus.ACTIVE.name());
        assertThat(saved[0].getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void rejectsDuplicateActiveSubscription() {
        Subscription active = Subscription.restore(2L, 1L, 3L, SubscriptionStatus.ACTIVE);
        SubscriptionService service = new SubscriptionService(
                new FakeLoadSubscriberPort(),
                (subscriberId, topicId) -> Optional.of(active),
                subscription -> subscription,
                new FakeTopicCatalog());

        assertThatThrownBy(() -> service.subscribe(1L, 3L))
                .isInstanceOf(ConflictException.class);
    }

    private static final class FakeLoadSubscriberPort implements LoadSubscriberPort {

        @Override
        public Optional<Subscriber> load(Long subscriberId) {
            return Optional.of(SUBSCRIBER);
        }

        @Override
        public Optional<Subscriber> loadByEmail(String email) {
            return Optional.empty();
        }
    }

    private record FakeTopicCatalog() implements TopicCatalog {

        @Override
        public Optional<TopicReference> findActive(Long topicId) {
            return Optional.of(new TopicReference(topicId));
        }
    }
}
