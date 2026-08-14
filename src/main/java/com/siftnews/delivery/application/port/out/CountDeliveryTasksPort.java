package com.siftnews.delivery.application.port.out;

public interface CountDeliveryTasksPort {

    int countByDeliveryJobId(Long deliveryJobId);
}
