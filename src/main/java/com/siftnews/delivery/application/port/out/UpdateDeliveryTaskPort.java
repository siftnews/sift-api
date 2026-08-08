package com.siftnews.delivery.application.port.out;

public interface UpdateDeliveryTaskPort {

    int claimPending(Long taskId);

    int markSent(Long taskId);

    int markFailed(Long taskId, String error);
}
