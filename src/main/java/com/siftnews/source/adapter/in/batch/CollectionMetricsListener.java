package com.siftnews.source.adapter.in.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 측정 베이스라인(하네스 원칙 5) — collectStep/collectionJob의 처리량(read/write count)과
 * 소요시간을 로깅한다. Spring Batch가 Micrometer 글로벌 레지스트리로 별도 발행하는
 * {@code spring.batch.*} 타이머와 함께, 사람이 읽는 배치 로그 라인을 남긴다.
 */
@Slf4j
class CollectionMetricsListener implements StepExecutionListener, JobExecutionListener {

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        Duration elapsed = elapsed(stepExecution.getStartTime(), stepExecution.getEndTime());
        long reads = stepExecution.getReadCount();
        long writes = stepExecution.getWriteCount();
        double throughput = throughputPerSecond(reads, elapsed);
        log.info(
                "[measure] step={} read={} write={} filter={} skip={} elapsedMs={} readThroughput/s={}",
                stepExecution.getStepName(), reads, writes,
                stepExecution.getFilterCount(), stepExecution.getSkipCount(),
                elapsed.toMillis(), String.format("%.2f", throughput));
        // Step 실패 시 원인 예외를 함께 남긴다 — 카운트만으론 실패 원인을 추적할 수 없다.
        if (!stepExecution.getFailureExceptions().isEmpty()) {
            log.warn("[measure] step={} 실패 원인={}", stepExecution.getStepName(),
                    stepExecution.getFailureExceptions().get(0).toString());
        }
        return stepExecution.getExitStatus();
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        Duration elapsed = elapsed(jobExecution.getStartTime(), jobExecution.getEndTime());
        log.info(
                "[measure] job={} status={} elapsedMs={}",
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getStatus(),
                elapsed.toMillis());
    }

    private static Duration elapsed(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return Duration.ZERO;
        }
        return Duration.between(start, end);
    }

    private static double throughputPerSecond(long reads, Duration elapsed) {
        long millis = elapsed.toMillis();
        return millis == 0 ? 0.0 : reads * 1000.0 / millis;
    }
}
