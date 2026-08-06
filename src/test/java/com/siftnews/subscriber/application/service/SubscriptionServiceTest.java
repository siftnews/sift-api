package com.siftnews.subscriber.application.service;

import com.siftnews.content.api.TopicReference;
import com.siftnews.subscriber.application.port.out.LoadSubscriberPort;
import com.siftnews.subscriber.domain.Subscriber;
import com.siftnews.subscriber.domain.SubscriberStatus;
import com.siftnews.subscriber.domain.Subscription;
import com.siftnews.subscriber.domain.SubscriptionStatus;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionServiceTest {
    private static final Subscriber SUBSCRIBER = Subscriber.restore(1L, "user@example.com", SubscriberStatus.ACTIVE, 8);

    @Test
    void subscribesToActiveTopic() {
        LoadSubscriberPort loadSubscriber = new LoadSubscriberPort() {
            public Optional<Subscriber> load(Long id) { return Optional.of(SUBSCRIBER); }
            public Optional<Subscriber> loadByEmail(String email) { return Optional.empty(); }
        };
        SubscriptionService service = new SubscriptionService(
                loadSubscriber, (subscriberId, topicId) -> Optional.empty(),
                subscription -> Subscription.restore(2L, subscription.getSubscriberId(), subscription.getTopicId(), subscription.getStatus()),
                topicId -> Optional.of(new TopicReference(topicId)));

        var result = service.subscribe(1L, 3L);

        assertThat(result.subscriptionId()).isEqualTo(2L);
        assertThat(result.status()).isEqualTo(SubscriptionStatus.ACTIVE.name());
    }

    @Test
    void unsubscribesExistingSubscription() {
        Subscription active = Subscription.restore(2L, 1L, 3L, SubscriptionStatus.ACTIVE);
        var saved = new Subscription[1];
        LoadSubscriberPort loadSubscriber = new LoadSubscriberPort() {
            public Optional<Subscriber> load(Long id) { return Optional.of(SUBSCRIBER); }
            public Optional<Subscriber> loadByEmail(String email) { return Optional.empty(); }
        };
        SubscriptionService service = new SubscriptionService(
                loadSubscriber, (subscriberId, topicId) -> Optional.of(active),
                subscription -> { saved[0] = subscription; return subscription; },
                topicId -> Optional.of(new TopicReference(topicId)));

        service.unsubscribe(1L, 3L);

        assertThat(saved[0].getStatus()).isEqualTo(SubscriptionStatus.PAUSED);
    }
}
