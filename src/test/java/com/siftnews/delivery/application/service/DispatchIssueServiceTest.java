package com.siftnews.delivery.application.service;

import com.siftnews.delivery.application.port.out.LoadDeliveryJobPort;
import com.siftnews.delivery.application.port.out.CountDeliveryTasksPort;
import com.siftnews.delivery.application.port.out.SaveDeliveryJobPort;
import com.siftnews.delivery.application.port.out.SaveDeliveryTaskPort;
import com.siftnews.delivery.application.port.out.UpdateDeliveryJobPort;
import com.siftnews.delivery.domain.DeliveryJob;
import com.siftnews.delivery.domain.DeliveryJobStatus;
import com.siftnews.delivery.domain.DeliveryTask;
import com.siftnews.delivery.domain.DeliveryTaskStatus;
import com.siftnews.subscriber.api.DeliveryRecipient;
import com.siftnews.subscriber.api.SubscriberCatalog;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class DispatchIssueServiceTest {

    @Test
    void 활성_수신자만_스냅샷으로_저장한다() {
        FakeJobs jobs = new FakeJobs();
        FakeTasks tasks = new FakeTasks();
        FakeCounts counts = new FakeCounts(1);
        FakeJobUpdates updates = new FakeJobUpdates();
        SubscriberCatalog recipients = (topicId, hour) -> List.of(new DeliveryRecipient(7L, "reader@example.com"));
        DispatchIssueService service = new DispatchIssueService(jobs, jobs, tasks, counts, updates, recipients);

        var summary = service.dispatch(3L, 2L, 9);

        assertThat(summary.deliveryJobId()).isEqualTo(1L);
        assertThat(summary.createdTaskCount()).isEqualTo(1);
        assertThat(updates.updatedJobId).isEqualTo(1L);
        assertThat(updates.updatedTotalCount).isEqualTo(1);
        assertThat(tasks.saved).singleElement().satisfies(task -> {
            assertThat(task.getSubscriberId()).isEqualTo(7L);
            assertThat(task.getStatus()).isEqualTo(DeliveryTaskStatus.PENDING);
        });
    }

    private static final class FakeJobs implements LoadDeliveryJobPort, SaveDeliveryJobPort {
        private DeliveryJob job;
        public Optional<DeliveryJob> loadByIssueId(Long issueId) { return Optional.ofNullable(job); }
        public DeliveryJob save(DeliveryJob deliveryJob) { job = DeliveryJob.restore(1L, deliveryJob.getIssueId(), 0, DeliveryJobStatus.CREATED); return job; }
    }

    private static final class FakeTasks implements SaveDeliveryTaskPort {
        private final List<DeliveryTask> saved = new ArrayList<>();
        public int saveIfAbsent(List<DeliveryTask> tasks) { saved.addAll(tasks); return tasks.size(); }
    }

    private record FakeCounts(int count) implements CountDeliveryTasksPort {

        @Override
        public int countByDeliveryJobId(Long deliveryJobId) {
            return count;
        }
    }

    private static final class FakeJobUpdates implements UpdateDeliveryJobPort {
        private Long updatedJobId;
        private int updatedTotalCount;

        @Override
        public int updateTotalCount(Long deliveryJobId, int totalCount) {
            updatedJobId = deliveryJobId;
            updatedTotalCount = totalCount;
            return 1;
        }

        @Override
        public int markSending(Long deliveryJobId) {
            return 1;
        }

        @Override
        public int markCompletedJobs() {
            return 0;
        }
    }
}
