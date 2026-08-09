package com.siftnews.delivery.application.port.out;

import com.siftnews.delivery.domain.DeliveryJob;

import java.util.Optional;

public interface LoadDeliveryJobPort {

    Optional<DeliveryJob> loadByIssueId(Long issueId);
}
