package com.siftnews.delivery.adapter.in.batch;

import com.siftnews.content.api.IssueCatalog;
import com.siftnews.delivery.application.port.in.DispatchJobRunner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.ZoneId;

@Slf4j
@Component
@Profile("!test")
class DispatchTrigger {

    private final DispatchJobRunner dispatchJobRunner;
    private final IssueCatalog issueCatalog;
    private final Clock clock;
    private final ZoneId zone;

    DispatchTrigger(DispatchJobRunner dispatchJobRunner,
                    IssueCatalog issueCatalog,
                    Clock clock,
                    @Value("${sift.delivery.zone:Asia/Seoul}") String zone) {
        this.dispatchJobRunner = dispatchJobRunner;
        this.issueCatalog = issueCatalog;
        this.clock = clock;
        this.zone = ZoneId.of(zone);
    }

    @Scheduled(cron = "${sift.delivery.cron:0 0 * * * *}", zone = "${sift.delivery.zone:Asia/Seoul}")
    void triggerHourlyDispatch() {
        var local = clock.instant().atZone(zone);
        for (var issue : issueCatalog.findScheduled(local.toLocalDate())) {
            try {
                var summary = dispatchJobRunner.run(issue.issueId(), issue.topicId(), local.getHour());
                log.info("dispatchJob 종료: issueId={} topicId={} executionId={} status={} exitCode={}",
                        issue.issueId(), issue.topicId(), summary.executionId(), summary.status(), summary.exitCode());
            } catch (Exception exception) {
                log.error("dispatchJob 기동 실패: issueId={}", issue.issueId(), exception);
            }
        }
    }
}
