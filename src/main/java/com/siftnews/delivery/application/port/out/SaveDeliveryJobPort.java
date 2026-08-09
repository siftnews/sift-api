package com.siftnews.delivery.application.port.out;

import com.siftnews.delivery.domain.DeliveryJob;

public interface SaveDeliveryJobPort {

    DeliveryJob save(DeliveryJob deliveryJob);
}
