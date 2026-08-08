package com.siftnews.delivery.application.port.out;

import java.time.Instant;

public interface UpdateDeliveryTaskPort {

    int claimPending(Long taskId);

    int claimFailed(Long taskId, Instant now, int maxAttempts);

    int markSent(Long taskId);

    int markFailed(Long taskId, String error, int attemptCount, Instant nextRetryAt);

    int markDead(Long taskId, String error, int attemptCount);
}
