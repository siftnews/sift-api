package com.siftnews.delivery.application.service;

import com.siftnews.delivery.domain.DeliveryException;
import com.siftnews.delivery.domain.DeliveryFailureCategory;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryRetryPolicyTest {

    private static final Instant NOW = Instant.parse("2026-08-08T03:00:00Z");
    private final DeliveryRetryPolicy policy = new DeliveryRetryPolicy(
            3, Duration.ofMinutes(1), Duration.ofMinutes(5));

    @Test
    void doublesDelayUntilMaximum() {
        assertThat(policy.nextRetryAt(NOW, 1)).isEqualTo(NOW.plus(Duration.ofMinutes(1)));
        assertThat(policy.nextRetryAt(NOW, 2)).isEqualTo(NOW.plus(Duration.ofMinutes(2)));
        assertThat(policy.nextRetryAt(NOW, 3)).isEqualTo(NOW.plus(Duration.ofMinutes(4)));
        assertThat(policy.nextRetryAt(NOW, 4)).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
    }

    @Test
    void retriesOnlyTransientDeliveryFailures() {
        assertThat(policy.isRetryable(new DeliveryException(DeliveryFailureCategory.TRANSIENT,
                new IllegalStateException()))).isTrue();
        assertThat(policy.isRetryable(new DeliveryException(DeliveryFailureCategory.PERMANENT,
                new IllegalStateException()))).isFalse();
    }
}
