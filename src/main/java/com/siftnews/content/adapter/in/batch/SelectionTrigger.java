package com.siftnews.content.adapter.in.batch;

import com.siftnews.content.application.port.out.LoadTopicPort;
import com.siftnews.content.domain.Topic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * 일일 발행 트리거 (MVP-DESIGN §3②) — 활성 토픽마다 selectionJob을 기동한다.
 * <p>
 * <b>윈도우 {@code [from, to)}를 한 번만 계산해 모든 토픽에 같은 값으로 넘긴다.</b> 이것이
 * D-032 불변식 (3)("한 runDate의 모든 selectionJob 실행은 동일한 윈도우를 공유")을 구조로
 * 보장하는 지점이다 — 각 Job이 스스로 "지금"을 기준으로 잡으면 먼저 도는 토픽과 나중 토픽의
 * 윈도우가 어긋나, 나중 토픽이 앞선 토픽의 스코어링 전제를 덮어쓴다.
 * <p>
 * 활성 토픽 목록은 port.out을 직접 부른다 — 배치 어댑터가 out-port에 직접 배선하는 것은
 * 수집 쪽과 같은 의도적 결정이다(MVP-DESIGN §3①).
 * <p>
 * {@code test} 프로파일에서는 뜨지 않는다 — 통합 테스트가 스케줄에 흔들리지 않도록.
 */
@Slf4j
@Component
@Profile("!test")
class SelectionTrigger {

    private final JobLauncher jobLauncher;
    private final Job selectionJob;
    private final LoadTopicPort loadTopicPort;
    private final Clock clock;
    private final Duration windowSize;
    private final ZoneId zone;

    SelectionTrigger(JobLauncher jobLauncher,
                     @Qualifier("selectionJob") Job selectionJob,
                     LoadTopicPort loadTopicPort,
                     Clock clock,
                     @Value("${sift.selection.window-hours:24}") int windowHours,
                     @Value("${sift.selection.zone:Asia/Seoul}") String zone) {
        this.jobLauncher = jobLauncher;
        this.selectionJob = selectionJob;
        this.loadTopicPort = loadTopicPort;
        this.clock = clock;
        this.windowSize = Duration.ofHours(windowHours);
        this.zone = ZoneId.of(zone);
    }

    /** 기본 매일 00:00 (D-019 "매일 발행 기준 시각"). 운영값은 설정으로 바꾼다. */
    @Scheduled(cron = "${sift.selection.cron:0 0 0 * * *}", zone = "${sift.selection.zone:Asia/Seoul}")
    void triggerDailySelection() {
        Instant to = clock.instant();
        Instant from = to.minus(windowSize);
        LocalDate runDate = LocalDate.ofInstant(to, zone);

        List<Topic> topics = loadTopicPort.loadActive();
        log.info("selectionTrigger 시작: runDate={} window=[{}, {}) 토픽={}건", runDate, from, to, topics.size());

        for (Topic topic : topics) {
            try {
                jobLauncher.run(selectionJob, parameters(topic.getTopicId(), runDate, from, to));
            } catch (Exception e) {
                // 한 토픽의 실패가 나머지 토픽의 발행을 막지 않는다 — 수집의 소스별 skip과 같은 격리.
                log.error("selectionJob 기동 실패: topicId={} runDate={}", topic.getTopicId(), runDate, e);
            }
        }
    }

    private JobParameters parameters(Long topicId, LocalDate runDate, Instant from, Instant to) {
        return new JobParametersBuilder()
                .addLong(SelectionJobParameters.TOPIC_ID, topicId)
                .addString(SelectionJobParameters.RUN_DATE, runDate.toString())
                .addString(SelectionJobParameters.WINDOW_FROM, from.toString())
                .addString(SelectionJobParameters.WINDOW_TO, to.toString())
                // 식별 파라미터로 둬야 같은 날 재기동이 "이미 완료된 인스턴스"로 거부되지 않는다.
                // 세 Step 모두 재실행 멱등이므로(D-031·upsert) 다시 돌아도 결과가 덧나지 않는다.
                .addLong(SelectionJobParameters.LAUNCHED_AT, clock.millis())
                .toJobParameters();
    }
}
