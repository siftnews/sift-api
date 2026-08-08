package com.siftnews.delivery.adapter.in.batch;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RetryMetricsListenerTest {

    @Test
    void recordsRetryStepAndJobMetricsWithBoundedStatusTags() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RetryMetricsListener listener = new RetryMetricsListener(meterRegistry);
        JobExecution jobExecution = new JobExecution(new JobInstance(1L, "retryJob"), new JobParameters());
        StepExecution stepExecution = new StepExecution("retryStep", jobExecution);
        stepExecution.setStartTime(LocalDateTime.parse("2026-08-08T03:00:00"));
        stepExecution.setEndTime(LocalDateTime.parse("2026-08-08T03:00:02"));
        stepExecution.setWriteCount(4);
        stepExecution.setExitStatus(org.springframework.batch.core.ExitStatus.COMPLETED);
        jobExecution.setStartTime(LocalDateTime.parse("2026-08-08T03:00:00"));
        jobExecution.setEndTime(LocalDateTime.parse("2026-08-08T03:00:02"));
        jobExecution.setStatus(BatchStatus.COMPLETED);

        listener.afterStep(stepExecution);
        listener.afterJob(jobExecution);

        assertThat(meterRegistry.find("sift.delivery.retry.duration").tag("status", "COMPLETED").timer())
                .isNotNull();
        assertThat(meterRegistry.find("sift.delivery.retry.tasks.processed")
                .tag("status", "COMPLETED").counter().count()).isEqualTo(4.0);
        assertThat(meterRegistry.find("sift.delivery.retry.job.duration")
                .tag("status", "COMPLETED").timer()).isNotNull();
    }

    @Test
    void marksRetryStepAsPartialFailureAndCountsFailedTasks() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RetryMetricsListener listener = new RetryMetricsListener(meterRegistry);
        JobExecution jobExecution = new JobExecution(new JobInstance(1L, "retryJob"), new JobParameters());
        StepExecution stepExecution = new StepExecution("retryStep", jobExecution);
        stepExecution.setStartTime(LocalDateTime.parse("2026-08-08T03:00:00"));
        stepExecution.setEndTime(LocalDateTime.parse("2026-08-08T03:00:02"));
        stepExecution.setWriteCount(4);
        stepExecution.setExitStatus(org.springframework.batch.core.ExitStatus.COMPLETED);
        stepExecution.getExecutionContext().putInt(DeliveryEmailWriter.FAILED_TASK_COUNT, 2);

        var exitStatus = listener.afterStep(stepExecution);

        assertThat(exitStatus.getExitCode()).isEqualTo("COMPLETED_WITH_ERRORS");
        assertThat(meterRegistry.find("sift.delivery.retry.tasks.processed")
                .tag("status", "COMPLETED_WITH_ERRORS").counter().count()).isEqualTo(4.0);
        assertThat(meterRegistry.find("sift.delivery.retry.tasks.failed")
                .tag("status", "COMPLETED_WITH_ERRORS").counter().count()).isEqualTo(2.0);
    }
}
