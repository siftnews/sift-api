package com.siftnews.subscriber.adapter.out.persistence;

import com.siftnews.subscriber.domain.Subscriber;

final class SubscriberMapper {

    private SubscriberMapper() {
    }

    static Subscriber toDomain(SubscriberJpaEntity entity) {
        return Subscriber.restore(entity.getId(), entity.getEmail(), entity.getStatus(),
                entity.getPreferredSendHour());
    }

    static SubscriberJpaEntity toEntity(Subscriber subscriber) {
        return new SubscriberJpaEntity(subscriber.getEmail(), subscriber.getStatus(),
                subscriber.getPreferredSendHour());
    }
}
