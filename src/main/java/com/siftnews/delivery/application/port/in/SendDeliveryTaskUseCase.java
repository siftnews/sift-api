package com.siftnews.delivery.application.port.in;

import com.siftnews.delivery.domain.DeliveryTask;

public interface SendDeliveryTaskUseCase {

    void send(DeliveryTask task);
}
