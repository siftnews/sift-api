package com.siftnews.delivery.adapter.in.batch;

import com.siftnews.delivery.application.port.out.LoadDeliveryJobPort;
import com.siftnews.delivery.application.port.out.LoadPendingDeliveryTasksPort;
import com.siftnews.delivery.domain.DeliveryJob;
import com.siftnews.delivery.domain.DeliveryJobStatus;
import com.siftnews.delivery.domain.DeliveryTask;
import com.siftnews.delivery.domain.DeliveryTaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PendingDeliveryTaskReaderTest {

    private static final Long ISSUE_ID = 11L;
    private static final Long JOB_ID = 5L;

    @Test
    void loadsBoundedPagesAndRestoresLastTaskIdOnRestart() {
        RecordingPendingTasks pendingTasks = new RecordingPendingTasks(List.of(
                task(101L), task(102L), task(103L), task(104L)));
        ExecutionContext executionContext = new ExecutionContext();
        PendingDeliveryTaskReader reader = new PendingDeliveryTaskReader(ISSUE_ID, fakeJobs(), pendingTasks, 2);

        reader.open(executionContext);
        assertThat(reader.read().getDeliveryTaskId()).isEqualTo(101L);
        assertThat(reader.read().getDeliveryTaskId()).isEqualTo(102L);
        reader.update(executionContext);

        assertThat(reader.read().getDeliveryTaskId()).isEqualTo(103L);
        assertThat(reader.read().getDeliveryTaskId()).isEqualTo(104L);
        assertThat(reader.read()).isNull();
        assertThat(pendingTasks.requests).containsExactly(
                new PageRequest(JOB_ID, null, 2),
                new PageRequest(JOB_ID, 102L, 2),
                new PageRequest(JOB_ID, 104L, 2));

        PendingDeliveryTaskReader restarted = new PendingDeliveryTaskReader(ISSUE_ID, fakeJobs(), pendingTasks, 2);
        restarted.open(executionContext);

        assertThat(restarted.read().getDeliveryTaskId()).isEqualTo(103L);
        assertThat(pendingTasks.requests.get(pendingTasks.requests.size() - 1))
                .isEqualTo(new PageRequest(JOB_ID, 102L, 2));
    }

    private static LoadDeliveryJobPort fakeJobs() {
        return issueId -> Optional.of(DeliveryJob.restore(JOB_ID, issueId, 4, DeliveryJobStatus.CREATED));
    }

    private static DeliveryTask task(Long taskId) {
        return DeliveryTask.restore(taskId, JOB_ID, ISSUE_ID, 7L, "reader@example.com",
                DeliveryTaskStatus.PENDING, "key-" + taskId);
    }

    private record PageRequest(
            Long jobId,
            Long afterTaskId,
            int limit
    ) {
    }

    private static final class RecordingPendingTasks implements LoadPendingDeliveryTasksPort {

        private final List<DeliveryTask> tasks;
        private final List<PageRequest> requests = new ArrayList<>();

        private RecordingPendingTasks(List<DeliveryTask> tasks) {
            this.tasks = tasks;
        }

        @Override
        public List<DeliveryTask> loadPending(Long deliveryJobId, Long afterTaskId, int limit) {
            requests.add(new PageRequest(deliveryJobId, afterTaskId, limit));
            return tasks.stream()
                    .filter(task -> afterTaskId == null || task.getDeliveryTaskId() > afterTaskId)
                    .limit(limit)
                    .toList();
        }
    }
}
