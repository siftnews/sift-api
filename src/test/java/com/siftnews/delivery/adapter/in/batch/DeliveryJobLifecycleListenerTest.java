package com.siftnews.delivery.adapter.in.batch;

import com.siftnews.delivery.application.port.out.LoadDeliveryJobPort;
import com.siftnews.delivery.application.port.out.UpdateDeliveryJobPort;
import com.siftnews.delivery.domain.DeliveryJob;
import com.siftnews.delivery.domain.DeliveryJobStatus;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryJobLifecycleListenerTest {

    @Test
    void marksJobSendingBeforeSendAndReconcilesAfterJob() {
        CapturingJobUpdates updates = new CapturingJobUpdates();
        LoadDeliveryJobPort jobs = issueId -> Optional.of(
                DeliveryJob.restore(55L, issueId, 1, DeliveryJobStatus.CREATED));
        DeliveryJobLifecycleListener listener = new DeliveryJobLifecycleListener(jobs, updates);
        var parameters = new JobParametersBuilder()
                .addLong(DispatchJobParameters.ISSUE_ID, 44L)
                .toJobParameters();
        JobExecution jobExecution = new JobExecution(new JobInstance(1L, "dispatchJob"), parameters);
        StepExecution stepExecution = new StepExecution("sendStep", jobExecution);

        listener.beforeStep(stepExecution);
        listener.afterJob(jobExecution);

        assertThat(updates.markedSendingJobId).isEqualTo(55L);
        assertThat(updates.markCompletedCalls).isEqualTo(1);
    }

    private static final class CapturingJobUpdates implements UpdateDeliveryJobPort {
        private Long markedSendingJobId;
        private int markCompletedCalls;

        @Override
        public int updateTotalCount(Long deliveryJobId, int totalCount) {
            return 1;
        }

        @Override
        public int markSending(Long deliveryJobId) {
            markedSendingJobId = deliveryJobId;
            return 1;
        }

        @Override
        public int markCompletedJobs() {
            markCompletedCalls++;
            return 1;
        }
    }
}
