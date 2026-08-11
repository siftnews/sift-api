package com.siftnews.delivery.adapter.out.email;

import com.siftnews.delivery.domain.DeliveryException;
import com.siftnews.delivery.domain.DeliveryFailureCategory;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoadtestSendEmailAdapterTest {

    @Test
    void recordsDeliveryMetricsWithoutRetainingEmailArguments() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        LoadtestSendEmailAdapter adapter = new LoadtestSendEmailAdapter(registry, 0, 0);

        adapter.send("reader@example.com", "Sift", "<p>private body</p>");

        assertThat(registry.get("sift.delivery.loadtest.mail.sent").counter().count())
                .isEqualTo(1);
        assertThat(registry.get("sift.delivery.loadtest.mail.failed").counter().count())
                .isEqualTo(0);
        assertThat(registry.get("sift.delivery.loadtest.mail.duration")
                .tag("status", "sent").timer().count()).isEqualTo(1);
        assertThat(registry.getMeters()).noneMatch(meter -> meter.getId().getName()
                .contains("reader@example.com"));
    }

    @Test
    void injectsDeterministicTransientFailureEveryConfiguredCall() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        LoadtestSendEmailAdapter adapter = new LoadtestSendEmailAdapter(registry, 2, 0);

        adapter.send("first@example.com", "Sift", "first");

        assertThatThrownBy(() -> adapter.send("second@example.com", "Sift", "second"))
                .isInstanceOfSatisfying(DeliveryException.class, exception ->
                        assertThat(exception.getCategory()).isEqualTo(DeliveryFailureCategory.TRANSIENT));

        assertThat(registry.get("sift.delivery.loadtest.mail.sent").counter().count())
                .isEqualTo(1);
        assertThat(registry.get("sift.delivery.loadtest.mail.failed").counter().count())
                .isEqualTo(1);
        assertThat(registry.get("sift.delivery.loadtest.mail.duration")
                .tag("status", "failed").timer().count()).isEqualTo(1);
    }

    @Test
    void rejectsNegativeConfiguration() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        assertThatThrownBy(() -> new LoadtestSendEmailAdapter(registry, -1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LoadtestSendEmailAdapter(registry, 0, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
