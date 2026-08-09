package com.siftnews.delivery.adapter.in.batch;

import com.siftnews.content.api.IssueCatalog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
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

    private final JobLauncher jobLauncher;
    private final Job dispatchJob;
    private final IssueCatalog issueCatalog;
    private final Clock clock;
    private final ZoneId zone;

    DispatchTrigger(JobLauncher jobLauncher,
                    @Qualifier("dispatchJob") Job dispatchJob,
                    IssueCatalog issueCatalog,
                    Clock clock,
                    @Value("${sift.delivery.zone:Asia/Seoul}") String zone) {
        this.jobLauncher = jobLauncher;
        this.dispatchJob = dispatchJob;
        this.issueCatalog = issueCatalog;
        this.clock = clock;
        this.zone = ZoneId.of(zone);
    }

    @Scheduled(cron = "${sift.delivery.cron:0 0 * * * *}", zone = "${sift.delivery.zone:Asia/Seoul}")
    void triggerHourlyDispatch() {
        var local = clock.instant().atZone(zone);
        for (var issue : issueCatalog.findScheduled(local.toLocalDate())) {
            try {
                jobLauncher.run(dispatchJob, new JobParametersBuilder()
                        .addLong(DispatchJobParameters.ISSUE_ID, issue.issueId())
                        .addLong(DispatchJobParameters.TOPIC_ID, issue.topicId())
                        .addLong(DispatchJobParameters.SEND_HOUR, (long) local.getHour())
                        .addLong(DispatchJobParameters.LAUNCHED_AT, clock.millis())
                        .toJobParameters());
            } catch (Exception exception) {
                log.error("dispatchJob 기동 실패: issueId={}", issue.issueId(), exception);
            }
        }
    }
}
