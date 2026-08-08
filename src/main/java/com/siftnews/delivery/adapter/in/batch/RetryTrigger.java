package com.siftnews.delivery.adapter.in.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Slf4j
@Component
@Profile("!test")
class RetryTrigger {

    private final JobLauncher jobLauncher;
    private final Job retryJob;
    private final Clock clock;

    RetryTrigger(JobLauncher jobLauncher, @Qualifier("retryJob") Job retryJob, Clock clock) {
        this.jobLauncher = jobLauncher;
        this.retryJob = retryJob;
        this.clock = clock;
    }

    @Scheduled(cron = "${sift.delivery.retry.cron:0 */5 * * * *}",
            zone = "${sift.delivery.retry.zone:Asia/Seoul}")
    void triggerRetry() {
        JobParameters parameters = new JobParametersBuilder()
                .addLong(RetryJobParameters.LAUNCHED_AT, clock.millis())
                .toJobParameters();
        log.info("retryJob 시작: launchedAt={}", parameters.getLong(RetryJobParameters.LAUNCHED_AT));
        try {
            var execution = jobLauncher.run(retryJob, parameters);
            log.info("retryJob 종료: executionId={} status={}", execution.getId(),
                    execution.getExitStatus().getExitCode());
        } catch (Exception exception) {
            log.error("retryJob 기동 실패 — 다음 주기에 다시 시도한다", exception);
        }
    }
}
