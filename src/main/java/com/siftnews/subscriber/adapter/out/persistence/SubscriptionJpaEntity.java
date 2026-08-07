package com.siftnews.subscriber.adapter.out.persistence;

import com.siftnews.common.BaseEntity;
import com.siftnews.subscriber.domain.SubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "subscription")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class SubscriptionJpaEntity extends BaseEntity {

    @Column(name = "subscriber_id", nullable = false)
    private Long subscriberId;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    SubscriptionJpaEntity(Long subscriberId, Long topicId, SubscriptionStatus status) {
        this.subscriberId = subscriberId;
        this.topicId = topicId;
        this.status = status;
    }

    void updateStatus(SubscriptionStatus status) {
        this.status = status;
    }
}
