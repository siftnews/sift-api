package com.siftnews.delivery.application.port.out;

import com.siftnews.delivery.domain.DeliveryTaskStatus;

public interface UpdateDeliveryTaskPort {

    void updateStatus(Long taskId, DeliveryTaskStatus status, String error);
}
