package com.siftnews.subscriber.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

interface SubscriptionJpaRepository extends JpaRepository<SubscriptionJpaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SubscriptionJpaEntity> findBySubscriberIdAndTopicId(Long subscriberId, Long topicId);
}
