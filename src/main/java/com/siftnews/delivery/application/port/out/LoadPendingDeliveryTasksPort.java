package com.siftnews.delivery.application.port.out;

import com.siftnews.delivery.domain.DeliveryTask;

import java.util.List;

public interface LoadPendingDeliveryTasksPort {

    List<DeliveryTask> loadPending(Long deliveryJobId);
}
