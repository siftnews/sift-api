package com.siftnews.source.adapter.in.batch;

import com.siftnews.source.application.port.in.CollectionJobRunSummary;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.JobParametersInvalidException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class SpringBatchCollectionJobRunnerTest {

    private static final Instant NOW = Instant.parse("2026-08-16T00:00:00Z");

    @Test
    void launchesTheSameCollectionJobWithAClockBasedIdentityParameter() throws Exception {
        RecordingJobLauncher launcher = new RecordingJobLauncher(execution());
        SpringBatchCollectionJobRunner runner = new SpringBatchCollectionJobRunner(
                launcher, new StubJob(), Clock.fixed(NOW, ZoneOffset.UTC));

        CollectionJobRunSummary summary = runner.run("run-1");

        assertThat(summary).isEqualTo(new CollectionJobRunSummary(
                "run-1", 42L, "COMPLETED", "COMPLETED", 10, 9, 1, 250));
        assertThat(launcher.job).isInstanceOf(StubJob.class);
        assertThat(launcher.parameters.getLong(CollectionJobParameters.LAUNCHED_AT))
                .isEqualTo(NOW.toEpochMilli());
        assertThat(launcher.parameters.getString(CollectionJobParameters.RUN_ID)).isEqualTo("run-1");
    }

    private static JobExecution execution() {
        JobParameters parameters = new JobParameters();
        JobExecution execution = new JobExecution(new JobInstance(42L, "collectionJob"), parameters);
        execution.setId(42L);
        execution.setStartTime(LocalDateTime.of(2026, 8, 16, 0, 0, 0));
        execution.setEndTime(LocalDateTime.of(2026, 8, 16, 0, 0, 0, 250_000_000));
        StepExecution step = execution.createStepExecution("collectStep");
        step.setReadCount(10);
        step.setWriteCount(9);
        step.setReadSkipCount(1);
        execution.setStatus(org.springframework.batch.core.BatchStatus.COMPLETED);
        execution.setExitStatus(org.springframework.batch.core.ExitStatus.COMPLETED);
        return execution;
    }

    private static final class RecordingJobLauncher implements JobLauncher {
        private final JobExecution execution;
        private Job job;
        private JobParameters parameters;

        private RecordingJobLauncher(JobExecution execution) {
            this.execution = execution;
        }

        @Override
        public JobExecution run(Job job, JobParameters jobParameters)
                throws JobExecutionAlreadyRunningException, JobRestartException,
                JobInstanceAlreadyCompleteException, JobParametersInvalidException {
            this.job = job;
            this.parameters = jobParameters;
            return execution;
        }
    }

    private static final class StubJob implements Job {
        @Override
        public String getName() {
            return "collectionJob";
        }

        @Override
        public void execute(JobExecution execution) {
            // runner contract test does not execute the real steps.
        }
    }
}
