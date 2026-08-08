package com.siftnews.delivery.adapter.in.batch;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
class RetryMetricsListener implements StepExecutionListener, JobExecutionListener {

    private final MeterRegistry meterRegistry;

    RetryMetricsListener(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        Duration elapsed = elapsed(stepExecution.getStartTime(), stepExecution.getEndTime());
        ExitStatus exitStatus = partialFailureStatus(stepExecution);
        String status = exitStatus.getExitCode();
        Timer.builder("sift.delivery.retry.duration")
                .tag("status", status)
                .register(meterRegistry)
                .record(elapsed);
        meterRegistry.counter("sift.delivery.retry.tasks.processed", "status", status)
                .increment(stepExecution.getWriteCount());
        meterRegistry.counter("sift.delivery.retry.tasks.failed", "status", status)
                .increment(stepExecution.getExecutionContext()
                        .getInt(DeliveryEmailWriter.FAILED_TASK_COUNT, 0));
        log.info("[measure] step={} status={} elapsedMs={}", stepExecution.getStepName(),
                status, elapsed.toMillis());
        if (!stepExecution.getFailureExceptions().isEmpty()) {
            log.warn("[measure] step={} 실패 원인={}", stepExecution.getStepName(),
                    stepExecution.getFailureExceptions().get(0).toString());
        }
        return exitStatus;
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        Duration elapsed = elapsed(jobExecution.getStartTime(), jobExecution.getEndTime());
        String status = jobExecution.getStatus().name();
        Timer.builder("sift.delivery.retry.job.duration")
                .tag("status", status)
                .register(meterRegistry)
                .record(elapsed);
        log.info("[measure] job={} status={} elapsedMs={}", jobExecution.getJobInstance().getJobName(),
                status, elapsed.toMillis());
    }

    private static Duration elapsed(LocalDateTime start, LocalDateTime end) {
        return start == null || end == null ? Duration.ZERO : Duration.between(start, end);
    }

    private static ExitStatus partialFailureStatus(StepExecution stepExecution) {
        ExitStatus exitStatus = stepExecution.getExitStatus();
        int failedTaskCount = stepExecution.getExecutionContext()
                .getInt(DeliveryEmailWriter.FAILED_TASK_COUNT, 0);
        if (failedTaskCount > 0
                && ExitStatus.COMPLETED.getExitCode().equals(exitStatus.getExitCode())) {
            return new ExitStatus("COMPLETED_WITH_ERRORS", "failedTasks=" + failedTaskCount);
        }
        return exitStatus;
    }
}
