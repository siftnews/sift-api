package com.siftnews.content.adapter.in.batch;

import com.siftnews.content.application.port.in.SelectionJobRunSummary;
import com.siftnews.content.domain.ContentException;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpringBatchSelectionJobRunnerTest {

    private static final Instant NOW = Instant.parse("2026-08-16T00:00:00Z");
    private static final Instant FROM = NOW.minusSeconds(3600);
    private static final Instant TO = NOW;

    @Test
    void launchesSelectionWithExplicitWindowAndAUniqueRunId() throws Exception {
        RecordingJobLauncher launcher = new RecordingJobLauncher(execution());
        SpringBatchSelectionJobRunner runner = new SpringBatchSelectionJobRunner(
                launcher, new StubJob(), Clock.fixed(NOW, ZoneOffset.UTC));

        SelectionJobRunSummary summary = runner.run(7L, LocalDate.of(2026, 8, 16), FROM, TO);

        assertThat(summary).isEqualTo(new SelectionJobRunSummary(
                43L, 7L, LocalDate.of(2026, 8, 16), FROM, TO, "COMPLETED", "COMPLETED", 500));
        assertThat(launcher.parameters.getLong(SelectionJobParameters.TOPIC_ID)).isEqualTo(7L);
        assertThat(launcher.parameters.getString(SelectionJobParameters.WINDOW_FROM)).isEqualTo(FROM.toString());
        assertThat(launcher.parameters.getString(SelectionJobParameters.WINDOW_TO)).isEqualTo(TO.toString());
        assertThat(launcher.parameters.getString(SelectionJobParameters.RUN_ID)).isNotBlank();
    }

    @Test
    void rejectsAnInvertedWindowBeforeLaunchingBatch() {
        RecordingJobLauncher launcher = new RecordingJobLauncher(execution());
        SpringBatchSelectionJobRunner runner = new SpringBatchSelectionJobRunner(
                launcher, new StubJob(), Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> runner.run(7L, LocalDate.of(2026, 8, 16), TO, FROM))
                .isInstanceOf(ContentException.class);
        assertThat(launcher.parameters).isNull();
    }

    private static JobExecution execution() {
        JobExecution execution = new JobExecution(new JobInstance(43L, "selectionJob"), new JobParameters());
        execution.setId(43L);
        execution.setStartTime(LocalDateTime.of(2026, 8, 16, 0, 0));
        execution.setEndTime(LocalDateTime.of(2026, 8, 16, 0, 0, 0, 500_000_000));
        execution.setStatus(org.springframework.batch.core.BatchStatus.COMPLETED);
        execution.setExitStatus(ExitStatus.COMPLETED);
        return execution;
    }

    private static final class RecordingJobLauncher implements JobLauncher {
        private final JobExecution execution;
        private JobParameters parameters;

        private RecordingJobLauncher(JobExecution execution) {
            this.execution = execution;
        }

        @Override
        public JobExecution run(Job job, JobParameters jobParameters)
                throws JobExecutionAlreadyRunningException, JobRestartException,
                JobInstanceAlreadyCompleteException, JobParametersInvalidException {
            this.parameters = jobParameters;
            return execution;
        }
    }

    private static final class StubJob implements Job {
        @Override
        public String getName() {
            return "selectionJob";
        }

        @Override
        public void execute(JobExecution execution) {
            // runner contract test does not execute the real steps.
        }
    }
}
