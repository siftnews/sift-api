package com.siftnews.delivery.application.service;

import com.siftnews.delivery.domain.DeliveryException;
import com.siftnews.delivery.domain.DeliveryFailureCategory;

import java.time.Duration;
import java.time.Instant;

public final class DeliveryRetryPolicy {

    private final int maxAttempts;
    private final Duration initialDelay;
    private final Duration maxDelay;

    public DeliveryRetryPolicy(int maxAttempts, Duration initialDelay, Duration maxDelay) {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("최대 발송 시도 횟수는 0보다 커야 합니다: " + maxAttempts);
        }
        if (initialDelay.isNegative() || initialDelay.isZero()) {
            throw new IllegalArgumentException("초기 재시도 지연은 0보다 커야 합니다: " + initialDelay);
        }
        if (maxDelay.compareTo(initialDelay) < 0) {
            throw new IllegalArgumentException("최대 재시도 지연은 초기 지연보다 작을 수 없습니다.");
        }
        this.maxAttempts = maxAttempts;
        this.initialDelay = initialDelay;
        this.maxDelay = maxDelay;
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    public boolean isRetryable(Throwable exception) {
        return DeliveryException.categoryOf(exception) == DeliveryFailureCategory.TRANSIENT;
    }

    public Instant nextRetryAt(Instant now, int attemptCount) {
        if (attemptCount <= 0) {
            throw new IllegalArgumentException("재시도 횟수는 0보다 커야 합니다: " + attemptCount);
        }
        long multiplier = 1L << Math.min(attemptCount - 1, 30);
        Duration delay;
        try {
            delay = initialDelay.multipliedBy(multiplier);
        } catch (ArithmeticException exception) {
            delay = maxDelay;
        }
        if (delay.compareTo(maxDelay) > 0) {
            delay = maxDelay;
        }
        return now.plus(delay);
    }
}
