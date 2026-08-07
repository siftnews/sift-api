package com.siftnews.delivery.adapter.out.persistence;

import com.siftnews.common.BaseEntity;
import com.siftnews.delivery.domain.DeliveryTaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "delivery_task")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class DeliveryTaskJpaEntity extends BaseEntity {

    @Column(name = "delivery_job_id", nullable = false)
    private Long deliveryJobId;

    @Column(name = "subscriber_id", nullable = false)
    private Long subscriberId;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryTaskStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    DeliveryTaskJpaEntity(Long deliveryJobId, Long subscriberId, String email,
                          DeliveryTaskStatus status, String idempotencyKey) {
        this.deliveryJobId = deliveryJobId;
        this.subscriberId = subscriberId;
        this.email = email;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
    }
}
