package com.siftnews.subscriber.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface SubscriberJpaRepository extends JpaRepository<SubscriberJpaEntity, Long> {

    Optional<SubscriberJpaEntity> findByEmail(String email);
}
