package com.siftnews.subscriber.adapter.out.persistence;

import com.siftnews.subscriber.domain.Subscription;

final class SubscriptionMapper {

    private SubscriptionMapper() {
    }

    static Subscription toDomain(SubscriptionJpaEntity entity) {
        return Subscription.restore(entity.getId(), entity.getSubscriberId(), entity.getTopicId(),
                entity.getStatus());
    }

    static SubscriptionJpaEntity toEntity(Subscription subscription) {
        return new SubscriptionJpaEntity(subscription.getSubscriberId(), subscription.getTopicId(),
                subscription.getStatus());
    }
}
