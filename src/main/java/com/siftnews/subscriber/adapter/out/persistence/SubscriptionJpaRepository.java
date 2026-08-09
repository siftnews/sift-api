package com.siftnews.subscriber.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.siftnews.subscriber.api.DeliveryRecipient;

import java.util.List;
import java.util.Optional;

interface SubscriptionJpaRepository extends JpaRepository<SubscriptionJpaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SubscriptionJpaEntity> findBySubscriberIdAndTopicId(Long subscriberId, Long topicId);

    @Query("""
            select new com.siftnews.subscriber.api.DeliveryRecipient(subscriber.id, subscriber.email)
            from SubscriptionJpaEntity subscription join SubscriberJpaEntity subscriber
              on subscriber.id = subscription.subscriberId
            where subscription.topicId = :topicId and subscription.status = 'ACTIVE'
              and subscriber.status = 'ACTIVE' and subscriber.preferredSendHour = :preferredSendHour
            """)
    List<DeliveryRecipient> findActiveRecipients(Long topicId, int preferredSendHour);
}
