package com.siftnews.source.adapter.in.batch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * collectionTrigger 단위 테스트 — 스케줄 자체가 아니라 <b>기동 계약</b>을 검증한다.
 * (cron 표현식은 설정값이고, Job 배선은 {@link CollectionJobIntegrationTest}가 본다)
 */
class CollectionTriggerTest {

    private static final Instant START = Instant.parse("2026-07-31T01:10:00Z");

    private TickingClock clock;
    private RecordingJobLauncher jobLauncher;
    private CollectionTrigger trigger;

    @BeforeEach
    void setUp() {
        clock = new TickingClock(START);
        jobLauncher = new RecordingJobLauncher();
        trigger = new CollectionTrigger(jobLauncher, new StubJob(), clock);
    }

    @Test
    void launchesCollectionJobWithLaunchedAtParameter() {
        trigger.triggerCollection();

        assertThat(jobLauncher.launches()).hasSize(1);
        assertThat(jobLauncher.launches().get(0).getLong(CollectionJobParameters.LAUNCHED_AT))
                .isEqualTo(START.toEpochMilli());
    }

    @Test
    void usesDistinctLaunchedAtPerRunSoEachCycleIsANewJobInstance() {
        trigger.triggerCollection();
        clock.advance(Duration.ofHours(1));
        trigger.triggerCollection();

        // 파라미터가 같으면 같은 JobInstance가 되어 두 번째 주기가 거부된다.
        // 그 거부를 실제 JobRepository로 확인하는 것은 CollectionJobIntegrationTest 몫.
        assertThat(jobLauncher.launches())
                .extracting(p -> p.getLong(CollectionJobParameters.LAUNCHED_AT))
                .containsExactly(START.toEpochMilli(), START.plus(Duration.ofHours(1)).toEpochMilli());
    }

    @Test
    void swallowsLaunchFailureSoTheSchedulerSurvivesToNextCycle() {
        jobLauncher.willThrow(new IllegalStateException("기동 실패(테스트)"));

        // 예외가 스케줄러 스레드까지 올라가면 이후 주기가 통째로 끊긴다.
        assertThatCode(() -> trigger.triggerCollection()).doesNotThrowAnyException();
    }

    static class RecordingJobLauncher implements JobLauncher {
        private final List<JobParameters> launches = new ArrayList<>();
        private RuntimeException failure;

        List<JobParameters> launches() {
            return launches;
        }

        void willThrow(RuntimeException e) {
            this.failure = e;
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

    static class StubJob implements Job {
        @Override
        public String getName() {
            return "collectionJob";
        }

        @Override
        public void execute(JobExecution execution) {
            // 기동 계약만 보는 테스트라 Job 본체는 돌리지 않는다.
        }
    }

    /** 주기마다 시각이 다름을 재현하기 위한 fake — {@code Clock.fixed}는 값이 고정돼 쓸 수 없다. */
    static class TickingClock extends Clock {
        private Instant now;

        TickingClock(Instant start) {
            this.now = start;
        }

        void advance(Duration amount) {
            now = now.plus(amount);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
