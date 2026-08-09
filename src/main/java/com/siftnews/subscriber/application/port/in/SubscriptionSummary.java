package com.siftnews.subscriber.application.port.in;

import com.siftnews.subscriber.domain.Subscription;

public record SubscriptionSummary(
        Long subscriptionId,
        Long subscriberId,
        Long topicId,
        String status
) {

    public static SubscriptionSummary from(Subscription subscription) {
        return new SubscriptionSummary(subscription.getSubscriptionId(), subscription.getSubscriberId(),
                subscription.getTopicId(), subscription.getStatus().name());
    }
}
