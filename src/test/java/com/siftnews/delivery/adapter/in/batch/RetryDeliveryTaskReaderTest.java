package com.siftnews.delivery.adapter.in.batch;

import com.siftnews.delivery.application.port.out.LoadRetriableDeliveryTasksPort;
import com.siftnews.delivery.domain.DeliveryTask;
import com.siftnews.delivery.domain.DeliveryTaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RetryDeliveryTaskReaderTest {

    private static final Instant NOW = Instant.parse("2026-08-08T03:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void loadsDueTasksInBoundedPagesAndRestoresCursor() {
        RecordingRetriableTasks tasks = new RecordingRetriableTasks(List.of(
                task(101L), task(102L), task(103L), task(104L)));
        ExecutionContext context = new ExecutionContext();
        RetryDeliveryTaskReader reader = new RetryDeliveryTaskReader(tasks, CLOCK, 3, 2);

        reader.open(context);
        assertThat(reader.read().getDeliveryTaskId()).isEqualTo(101L);
        assertThat(reader.read().getDeliveryTaskId()).isEqualTo(102L);
        reader.update(context);
        assertThat(reader.read().getDeliveryTaskId()).isEqualTo(103L);
        assertThat(reader.read().getDeliveryTaskId()).isEqualTo(104L);
        assertThat(reader.read()).isNull();

        assertThat(tasks.requests).containsExactly(
                new PageRequest(NOW, null, 3, 2),
                new PageRequest(NOW, 102L, 3, 2),
                new PageRequest(NOW, 104L, 3, 2));

        RetryDeliveryTaskReader restarted = new RetryDeliveryTaskReader(tasks, CLOCK, 3, 2);
        restarted.open(context);
        assertThat(restarted.read().getDeliveryTaskId()).isEqualTo(103L);
        assertThat(tasks.requests.get(tasks.requests.size() - 1))
                .isEqualTo(new PageRequest(NOW, 102L, 3, 2));
    }

    private static DeliveryTask task(Long taskId) {
        return DeliveryTask.restore(taskId, 5L, 11L, 7L, "reader@example.com", DeliveryTaskStatus.FAILED,
                1, NOW.minusSeconds(1), "temporary failure", "key-" + taskId);
    }

    private record PageRequest(
            Instant now,
            Long afterTaskId,
            int maxAttempts,
            int limit
    ) {
    }

    private static final class RecordingRetriableTasks implements LoadRetriableDeliveryTasksPort {

        private final List<DeliveryTask> tasks;
        private final List<PageRequest> requests = new ArrayList<>();

        private RecordingRetriableTasks(List<DeliveryTask> tasks) {
            this.tasks = tasks;
        }

        @Override
        public List<DeliveryTask> loadRetriable(Instant now, Long afterTaskId, int maxAttempts, int limit) {
            requests.add(new PageRequest(now, afterTaskId, maxAttempts, limit));
            return tasks.stream()
                    .filter(task -> afterTaskId == null || task.getDeliveryTaskId() > afterTaskId)
                    .limit(limit)
                    .toList();
        }
    }
}
