package com.siftnews.delivery.adapter.in.batch;

import com.siftnews.delivery.application.port.in.CompleteDeliveryJobsUseCase;
import com.siftnews.delivery.application.port.in.MarkDeliveryJobSendingUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryJobLifecycleListenerTest {

    @Test
    void marksJobSendingBeforeSendAndReconcilesAfterJob() {
        CapturingLifecycleUseCases useCases = new CapturingLifecycleUseCases();
        DeliveryJobLifecycleListener listener = new DeliveryJobLifecycleListener(useCases, useCases);
        var parameters = new JobParametersBuilder()
                .addLong(DispatchJobParameters.ISSUE_ID, 44L)
                .toJobParameters();
        JobExecution jobExecution = new JobExecution(new JobInstance(1L, "dispatchJob"), parameters);
        StepExecution stepExecution = new StepExecution("sendStep", jobExecution);

        listener.beforeStep(stepExecution);
        listener.afterJob(jobExecution);

        assertThat(useCases.markedSendingIssueId).isEqualTo(44L);
        assertThat(useCases.completeCalls).isEqualTo(1);
    }

    private static final class CapturingLifecycleUseCases
            implements MarkDeliveryJobSendingUseCase, CompleteDeliveryJobsUseCase {
        private Long markedSendingIssueId;
        private int completeCalls;

        @Override
        public void markSending(Long issueId) {
            markedSendingIssueId = issueId;
        }

        @Override
        public void complete() {
            completeCalls++;
        }
    }
}
