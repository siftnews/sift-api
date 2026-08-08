package com.siftnews.delivery.adapter.in.batch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class RetryTriggerTest {

    private static final Instant NOW = Instant.parse("2026-08-08T03:00:00Z");

    private RecordingJobLauncher jobLauncher;
    private RetryTrigger trigger;

    @BeforeEach
    void setUp() {
        jobLauncher = new RecordingJobLauncher();
        trigger = new RetryTrigger(jobLauncher, new StubJob(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void launchesRetryJobWithUniqueTimestampParameter() {
        trigger.triggerRetry();

        assertThat(jobLauncher.launches()).singleElement()
                .satisfies(parameters -> assertThat(parameters.getLong(RetryJobParameters.LAUNCHED_AT))
                        .isEqualTo(NOW.toEpochMilli()));
    }

    @Test
    void swallowsLaunchFailureSoTheSchedulerSurvives() {
        jobLauncher.willThrow(new IllegalStateException("기동 실패(테스트)"));

        assertThatCode(() -> trigger.triggerRetry()).doesNotThrowAnyException();
    }

    private static final class RecordingJobLauncher implements JobLauncher {

        private final List<JobParameters> launches = new ArrayList<>();
        private RuntimeException failure;

        List<JobParameters> launches() {
            return launches;
        }

        void willThrow(RuntimeException exception) {
            failure = exception;
        }

        @Override
        public JobExecution run(Job job, JobParameters jobParameters) {
            if (failure != null) {
                throw failure;
            }
            launches.add(jobParameters);
            return new JobExecution(1L);
        }
    }

    private static final class StubJob implements Job {

        @Override
        public String getName() {
            return "retryJob";
        }

        @Override
        public void execute(JobExecution execution) {
        }
    }
}
