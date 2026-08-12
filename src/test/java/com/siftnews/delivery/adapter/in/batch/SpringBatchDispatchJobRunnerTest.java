package com.siftnews.delivery.adapter.in.batch;

import com.siftnews.delivery.application.port.in.DispatchJobRunSummary;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpringBatchDispatchJobRunnerTest {

    private static final Instant NOW = Instant.parse("2026-08-08T03:00:00Z");

    @Test
    void launchesDispatchJobAndMapsExecutionSummary() throws Exception {
        JobExecution execution = completedExecution();
        RecordingJobLauncher jobLauncher = new RecordingJobLauncher(execution);
        SpringBatchDispatchJobRunner runner = new SpringBatchDispatchJobRunner(
                jobLauncher, new StubJob(), Clock.fixed(NOW, ZoneOffset.UTC));

        DispatchJobRunSummary summary = runner.run(11L, 12L, 9);

        assertThat(summary).isEqualTo(new DispatchJobRunSummary(
                42L, "COMPLETED", "COMPLETED", 3, 2, 1));
        assertThat(jobLauncher.launches()).singleElement().satisfies(parameters -> {
            assertThat(parameters.getLong(DispatchJobParameters.ISSUE_ID)).isEqualTo(11L);
            assertThat(parameters.getLong(DispatchJobParameters.TOPIC_ID)).isEqualTo(12L);
            assertThat(parameters.getLong(DispatchJobParameters.SEND_HOUR)).isEqualTo(9L);
            assertThat(parameters.getLong(DispatchJobParameters.LAUNCHED_AT)).isEqualTo(NOW.toEpochMilli());
            assertThat(parameters.getString(DispatchJobParameters.RUN_ID)).isNotBlank();
        });
    }

    @Test
    void addsDistinctRunIdWhenRequestsShareTheSameMillisecond() throws Exception {
        RecordingJobLauncher jobLauncher = new RecordingJobLauncher(completedExecution());
        SpringBatchDispatchJobRunner runner = new SpringBatchDispatchJobRunner(
                jobLauncher, new StubJob(), Clock.fixed(NOW, ZoneOffset.UTC));

        runner.run(11L, 12L, 9);
        runner.run(11L, 12L, 9);

        assertThat(jobLauncher.launches())
                .extracting(parameters -> parameters.getString(DispatchJobParameters.RUN_ID))
                .doesNotHaveDuplicates();
    }

    private static JobExecution completedExecution() {
        JobExecution execution = new JobExecution(new JobInstance(42L, "dispatchJob"), 42L,
                new JobParameters());
        execution.setStatus(BatchStatus.COMPLETED);
        execution.setExitStatus(ExitStatus.COMPLETED);

        StepExecution snapshotStep = execution.createStepExecution("snapshotStep");
        snapshotStep.getExecutionContext().putInt(DispatchMetricsListener.CREATED_TASK_COUNT, 3);

        StepExecution sendStep = execution.createStepExecution("sendStep");
        sendStep.setWriteCount(2);
        sendStep.getExecutionContext().putInt(DeliveryEmailWriter.FAILED_TASK_COUNT, 1);
        return execution;
    }

    private static final class RecordingJobLauncher implements JobLauncher {

        private final JobExecution execution;
        private final List<JobParameters> launches = new ArrayList<>();

        private RecordingJobLauncher(JobExecution execution) {
            this.execution = execution;
        }

        @Override
        public JobExecution run(Job job, JobParameters jobParameters) {
            launches.add(jobParameters);
            return execution;
        }

        private List<JobParameters> launches() {
            return launches;
        }
    }

    private static final class StubJob implements Job {

        @Override
        public String getName() {
            return "dispatchJob";
        }

        @Override
        public void execute(JobExecution execution) {
        }
    }
}
