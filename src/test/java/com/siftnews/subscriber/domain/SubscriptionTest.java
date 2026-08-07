package com.siftnews.subscriber.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionTest {
    @Test
    void unsubscribeChangesStatusToPaused() {
        Subscription subscription = Subscription.create(1L, 2L);

        assertThat(subscription.unsubscribe().getStatus()).isEqualTo(SubscriptionStatus.PAUSED);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.unsubscribe().reactivate().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }
}
