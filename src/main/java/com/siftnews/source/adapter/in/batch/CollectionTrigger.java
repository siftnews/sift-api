package com.siftnews.source.adapter.in.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * 수집 주기 트리거 (MVP-DESIGN §3①) — collectionJob을 주기 기동한다.
 * <p>
 * {@code spring.batch.job.enabled=false}라 부팅으로는 Job이 뜨지 않는다. 선별 쪽
 * {@code SelectionTrigger}와 달리 수집은 대상이 "전체 활성 소스" 하나뿐이라 루프가 없고,
 * 소스 목록은 Job의 reader가 매 실행 읽는다.
 * <p>
 * {@code test} 프로파일에서는 뜨지 않는다 — 통합 테스트가 스케줄에 흔들리지 않도록
 * ({@code SelectionTrigger}와 동일).
 */
@Slf4j
@Component
@Profile("!test")
class CollectionTrigger {

    private final JobLauncher jobLauncher;
    private final Job collectionJob;
    private final Clock clock;

    CollectionTrigger(JobLauncher jobLauncher,
                      @Qualifier("collectionJob") Job collectionJob,
                      Clock clock) {
        this.jobLauncher = jobLauncher;
        this.collectionJob = collectionJob;
        this.clock = clock;
    }

    /**
     * 기본 매시 10분. 매시 정각은 수집기가 몰리는 시각이라 비켜 둔다.
     * 운영값은 {@code sift.collection.cron}으로 바꾼다.
     */
    @Scheduled(cron = "${sift.collection.cron:0 10 * * * *}", zone = "${sift.collection.zone:Asia/Seoul}")
    void triggerCollection() {
        JobParameters parameters = parameters();
        log.info("collectionTrigger 시작: launchedAt={}", parameters.getLong(CollectionJobParameters.LAUNCHED_AT));

        try {
            JobExecution execution = jobLauncher.run(collectionJob, parameters);
            log.info("collectionJob 종료: executionId={} status={}", execution.getId(), execution.getStatus());
        } catch (Exception e) {
            // 기동 실패로 스케줄러 스레드를 죽이지 않는다 — 다음 주기에 다시 시도한다.
            // (소스별 fetch 실패는 여기까지 오지 않고 collectStep의 skip으로 격리된다)
            log.error("collectionJob 기동 실패 — 다음 주기에 다시 시도한다", e);
        }
    }

    private JobParameters parameters() {
        return new JobParametersBuilder()
                // 식별 파라미터로 둬야 매 주기 기동이 새 JobInstance가 된다.
                // 수집은 중복 무시 저장이라 재실행이 멱등이다 (CollectionJobParameters 참조).
                .addLong(CollectionJobParameters.LAUNCHED_AT, clock.millis())
                .toJobParameters();
    }
}
