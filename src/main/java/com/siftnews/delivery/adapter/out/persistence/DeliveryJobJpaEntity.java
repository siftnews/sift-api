package com.siftnews.delivery.adapter.out.persistence;

import com.siftnews.common.BaseEntity;
import com.siftnews.delivery.domain.DeliveryJobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "delivery_job")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class DeliveryJobJpaEntity extends BaseEntity {

    @Column(name = "issue_id", nullable = false, unique = true)
    private Long issueId;

    @Column(name = "total_count", nullable = false)
    private int totalCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryJobStatus status;

    DeliveryJobJpaEntity(Long issueId, int totalCount, DeliveryJobStatus status) {
        this.issueId = issueId;
        this.totalCount = totalCount;
        this.status = status;
    }
}
