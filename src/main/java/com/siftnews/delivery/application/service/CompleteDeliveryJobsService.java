package com.siftnews.delivery.application.service;

import com.siftnews.delivery.application.port.in.CompleteDeliveryJobsUseCase;
import com.siftnews.delivery.application.port.out.UpdateDeliveryJobPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class CompleteDeliveryJobsService implements CompleteDeliveryJobsUseCase {

    private final UpdateDeliveryJobPort updateDeliveryJobPort;

    @Override
    public void complete() {
        updateDeliveryJobPort.markCompletedJobs();
    }
}
