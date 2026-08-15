package com.siftnews.source.adapter.in.batch;

import lombok.extern.slf4j.Slf4j;
import com.siftnews.source.application.port.in.CollectionJobRunSummary;
import com.siftnews.source.application.port.in.CollectionJobRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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

    private final CollectionJobRunner collectionJobRunner;

    CollectionTrigger(CollectionJobRunner collectionJobRunner) {
        this.collectionJobRunner = collectionJobRunner;
    }

    /**
     * 기본 매시 10분. 매시 정각은 수집기가 몰리는 시각이라 비켜 둔다.
     * 운영값은 {@code sift.collection.cron}으로 바꾼다.
     */
    @Scheduled(cron = "${sift.collection.cron:0 10 * * * *}", zone = "${sift.collection.zone:Asia/Seoul}")
    void triggerCollection() {
        try {
            CollectionJobRunSummary summary = collectionJobRunner.run();
            log.info("collectionTrigger 종료: executionId={} status={}", summary.executionId(), summary.status());
        } catch (Exception e) {
            // 기동 실패로 스케줄러 스레드를 죽이지 않는다 — 다음 주기에 다시 시도한다.
            // (소스별 fetch 실패는 여기까지 오지 않고 collectStep의 skip으로 격리된다)
            log.error("collectionJob 기동 실패 — 다음 주기에 다시 시도한다", e);
        }
    }
}
