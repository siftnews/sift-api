package com.siftnews.delivery.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface DeliveryTaskJpaRepository extends JpaRepository<DeliveryTaskJpaEntity, Long> {
}
