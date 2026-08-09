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
        if (subscription.getSubscriptionId() != null) {
            return new SubscriptionJpaEntity(subscription.getSubscriptionId(),
                    subscription.getSubscriberId(), subscription.getTopicId(), subscription.getStatus());
        }
        return new SubscriptionJpaEntity(subscription.getSubscriberId(), subscription.getTopicId(),
                subscription.getStatus());
    }
}
