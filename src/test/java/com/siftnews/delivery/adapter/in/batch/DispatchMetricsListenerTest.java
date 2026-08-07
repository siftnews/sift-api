package com.siftnews.delivery.adapter.in.batch;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DispatchMetricsListenerTest {

    @Test
    void recordsSnapshotDurationAndCreatedTaskCountWithStatusTag() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        DispatchMetricsListener listener = new DispatchMetricsListener(meterRegistry);
        JobExecution jobExecution = new JobExecution(new JobInstance(1L, "dispatchJob"), new JobParameters());
        StepExecution stepExecution = new StepExecution("snapshotStep", jobExecution);
        stepExecution.setStartTime(LocalDateTime.parse("2026-08-07T09:00:00"));
        stepExecution.setEndTime(LocalDateTime.parse("2026-08-07T09:00:02"));
        stepExecution.setExitStatus(ExitStatus.COMPLETED);
        stepExecution.getExecutionContext().putInt(DispatchMetricsListener.CREATED_TASK_COUNT, 3);

        listener.afterStep(stepExecution);

        assertThat(meterRegistry.find("sift.delivery.snapshot.duration").tag("status", "COMPLETED").timer())
                .isNotNull();
        assertThat(meterRegistry.find("sift.delivery.tasks.created").tag("status", "COMPLETED").counter().count())
                .isEqualTo(3.0);
    }

    @Test
    void recordsJobDurationWithBoundedStatusTag() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        DispatchMetricsListener listener = new DispatchMetricsListener(meterRegistry);
        JobExecution jobExecution = new JobExecution(new JobInstance(1L, "dispatchJob"), new JobParameters());
        jobExecution.setStartTime(LocalDateTime.parse("2026-08-07T09:00:00"));
        jobExecution.setEndTime(LocalDateTime.parse("2026-08-07T09:00:03"));
        jobExecution.setStatus(BatchStatus.COMPLETED);

        listener.afterJob(jobExecution);

        assertThat(meterRegistry.find("sift.delivery.dispatch.duration").tag("status", "COMPLETED").timer())
                .isNotNull();
    }
}
