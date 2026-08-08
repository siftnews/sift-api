package com.siftnews.delivery.adapter.in.batch;

import com.siftnews.delivery.application.port.in.SendDeliveryTaskUseCase;
import com.siftnews.delivery.domain.DeliveryTask;
import com.siftnews.delivery.domain.DeliveryTaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.Chunk;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryEmailWriterTest {

    @Test
    void delegatesEachTaskToApplicationUseCase() throws Exception {
        RecordingSendDeliveryTaskUseCase useCase = new RecordingSendDeliveryTaskUseCase();
        DeliveryEmailWriter writer = new DeliveryEmailWriter(useCase);
        DeliveryTask first = task(101L, "reader1@example.com");
        DeliveryTask second = task(102L, "reader2@example.com");

        writer.write(new Chunk<>(List.of(first, second)));

        assertThat(useCase.tasks).containsExactly(first, second);
    }

    private static DeliveryTask task(Long taskId, String email) {
        return DeliveryTask.restore(taskId, 5L, 11L, 7L, email, DeliveryTaskStatus.PENDING, "key-" + taskId);
    }

    private static final class RecordingSendDeliveryTaskUseCase implements SendDeliveryTaskUseCase {

        private final List<DeliveryTask> tasks = new ArrayList<>();

        @Override
        public void send(DeliveryTask task) {
            tasks.add(task);
        }
    }
}
