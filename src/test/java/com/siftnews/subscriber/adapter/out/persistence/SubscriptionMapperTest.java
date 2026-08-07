package com.siftnews.subscriber.adapter.out.persistence;

import com.siftnews.subscriber.domain.Subscription;
import com.siftnews.subscriber.domain.SubscriptionStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionMapperTest {

    @Test
    void preservesSubscriptionIdWhenMappingRestoredSubscription() {
        Subscription subscription = Subscription.restore(7L, 1L, 3L, SubscriptionStatus.PAUSED);

        SubscriptionJpaEntity entity = SubscriptionMapper.toEntity(subscription);

        assertThat(entity.getId()).isEqualTo(7L);
        assertThat(entity.getSubscriberId()).isEqualTo(1L);
        assertThat(entity.getTopicId()).isEqualTo(3L);
        assertThat(entity.getStatus()).isEqualTo(SubscriptionStatus.PAUSED);
    }

    @Test
    void leavesSubscriptionIdUnsetForNewSubscription() {
        Subscription subscription = Subscription.create(1L, 3L);

        SubscriptionJpaEntity entity = SubscriptionMapper.toEntity(subscription);

        assertThat(entity.getId()).isNull();
    }
}
