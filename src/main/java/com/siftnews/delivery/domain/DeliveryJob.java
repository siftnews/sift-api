package com.siftnews.delivery.domain;

import com.siftnews.common.BusinessException;
import lombok.Getter;

@Getter
public class DeliveryJob {

    private final Long deliveryJobId;
    private final Long issueId;
    private final int totalCount;
    private final DeliveryJobStatus status;

    private DeliveryJob(Long deliveryJobId, Long issueId, int totalCount, DeliveryJobStatus status) {
        this.deliveryJobId = deliveryJobId;
        this.issueId = issueId;
        this.totalCount = totalCount;
        this.status = status;
    }

    public static DeliveryJob create(Long issueId) {
        if (issueId == null) {
            throw new BusinessException("delivery job의 issueId는 필수입니다.");
        }
        return new DeliveryJob(null, issueId, 0, DeliveryJobStatus.CREATED);
    }

    public static DeliveryJob restore(Long deliveryJobId, Long issueId, int totalCount, DeliveryJobStatus status) {
        return new DeliveryJob(deliveryJobId, issueId, totalCount, status);
    }
}
