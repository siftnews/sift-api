package com.siftnews.subscriber.domain;

import lombok.Getter;

@Getter
public class Subscription {

    private final Long subscriptionId;
    private final Long subscriberId;
    private final Long topicId;
    private final SubscriptionStatus status;

    private Subscription(Long subscriptionId, Long subscriberId, Long topicId, SubscriptionStatus status) {
        this.subscriptionId = subscriptionId;
        this.subscriberId = subscriberId;
        this.topicId = topicId;
        this.status = status;
    }

    public static Subscription create(Long subscriberId, Long topicId) {
        if (subscriberId == null || topicId == null) {
            throw new SubscriberException("subscriberId와 topicId는 필수입니다.");
        }

        return new Subscription(null, subscriberId, topicId, SubscriptionStatus.ACTIVE);
    }

    public static Subscription restore(
            Long subscriptionId,
            Long subscriberId,
            Long topicId,
            SubscriptionStatus status
    ) {
        return new Subscription(subscriptionId, subscriberId, topicId, status);
    }

    public Subscription unsubscribe() {
        return new Subscription(subscriptionId, subscriberId, topicId, SubscriptionStatus.PAUSED);
    }

    public Subscription reactivate() {
        return new Subscription(subscriptionId, subscriberId, topicId, SubscriptionStatus.ACTIVE);
    }
}
