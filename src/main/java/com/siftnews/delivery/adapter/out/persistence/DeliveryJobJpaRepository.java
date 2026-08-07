package com.siftnews.delivery.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface DeliveryJobJpaRepository extends JpaRepository<DeliveryJobJpaEntity, Long> {

    Optional<DeliveryJobJpaEntity> findByIssueId(Long issueId);
}
