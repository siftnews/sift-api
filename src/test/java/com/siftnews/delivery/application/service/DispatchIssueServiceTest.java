package com.siftnews.delivery.application.service;

import com.siftnews.delivery.application.port.out.*;
import com.siftnews.delivery.domain.*;
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
        SubscriberCatalog recipients = (topicId, hour) -> List.of(new DeliveryRecipient(7L, "reader@example.com"));
        DispatchIssueService service = new DispatchIssueService(jobs, jobs, tasks, recipients);

        Long jobId = service.dispatch(3L, 2L, 9);

        assertThat(jobId).isEqualTo(1L);
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
}
