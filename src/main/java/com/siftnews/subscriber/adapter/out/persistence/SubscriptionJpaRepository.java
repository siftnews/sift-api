package com.siftnews.subscriber.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface SubscriptionJpaRepository extends JpaRepository<SubscriptionJpaEntity, Long> {

    Optional<SubscriptionJpaEntity> findBySubscriberIdAndTopicId(Long subscriberId, Long topicId);
}
