package com.siftnews.delivery.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryTaskTest {

    @Test
    void 같은_이슈와_구독자는_같은_멱등_키를_쓴다() {
        DeliveryTask first = DeliveryTask.pending(1L, 10L, 100L, "reader@example.com");
        DeliveryTask retry = DeliveryTask.pending(2L, 10L, 100L, "reader@example.com");

        assertThat(first.getIdempotencyKey()).isEqualTo(retry.getIdempotencyKey());
    }
}
