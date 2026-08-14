package com.siftnews.delivery.application.port.out;

public interface UpdateDeliveryJobPort {

    int updateTotalCount(Long deliveryJobId, int totalCount);

    int markSending(Long deliveryJobId);

    int markCompletedJobs();
}
