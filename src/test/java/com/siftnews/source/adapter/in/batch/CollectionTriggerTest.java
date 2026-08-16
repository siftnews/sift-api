package com.siftnews.source.adapter.in.batch;

import com.siftnews.source.application.port.in.CollectionJobRunSummary;
import com.siftnews.source.application.port.in.CollectionJobRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * collectionTrigger 단위 테스트 — 스케줄 자체가 아니라 <b>공유 실행기 위임 계약</b>을 검증한다.
 */
class CollectionTriggerTest {

    private RecordingCollectionJobRunner jobRunner;
    private CollectionTrigger trigger;

    @BeforeEach
    void setUp() {
        jobRunner = new RecordingCollectionJobRunner();
        trigger = new CollectionTrigger(jobRunner);
    }

    @Test
    void runsCollectionJobThroughTheSharedRunner() {
        trigger.triggerCollection();

        assertThat(jobRunner.runs).isEqualTo(1);
    }

    @Test
    void swallowsLaunchFailureSoTheSchedulerSurvivesToNextCycle() {
        jobRunner.willThrow(new IllegalStateException("기동 실패(테스트)"));

        // 예외가 스케줄러 스레드까지 올라가면 이후 주기가 통째로 끊긴다.
        assertThatCode(() -> trigger.triggerCollection()).doesNotThrowAnyException();
    }

    static class RecordingCollectionJobRunner implements CollectionJobRunner {
        private int runs;
        private RuntimeException failure;

        void willThrow(RuntimeException e) {
            this.failure = e;
        }

        @Override
        public CollectionJobRunSummary run() {
            if (failure != null) {
                throw failure;
            }
            runs++;
            return new CollectionJobRunSummary("schedule", 1L, "COMPLETED", "COMPLETED", 10, 10, 0, 100);
        }

        @Override
        public CollectionJobRunSummary run(String runId) {
            return run();
        }
    }
}
