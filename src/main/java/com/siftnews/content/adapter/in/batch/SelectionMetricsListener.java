package com.siftnews.content.adapter.in.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 측정 베이스라인(하네스 원칙 5) — selectionJob의 step별 소요시간과 Job 결과를 남긴다.
 * <p>
 * 계측이 애플리케이션 서비스가 아니라 <b>배치 어댑터</b>에 있는 이유: 서비스는 배치를 몰라야 하고
 * (헥사고날), 수집 쪽 {@code CollectionMetricsListener}도 같은 자리다. PR #26 리뷰에서
 * "서비스에 Micrometer를 붙이라"는 제안을 이 근거로 반박하고 여기에 넣기로 정리했다.
 * <p>
 * 세 Step 모두 tasklet이라 read/write count가 0이다 — 대신 각 Step이 처리한 건수는
 * 서비스가 자기 로그로 남기고(예: {@code 스코어링 완료: loaded=… scored=…}), 여기서는
 * <b>단계별 소요시간</b>을 책임진다. chunk로 전환하면 카운트도 여기서 나온다.
 */
@Slf4j
class SelectionMetricsListener implements StepExecutionListener, JobExecutionListener {

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        Duration elapsed = elapsed(stepExecution.getStartTime(), stepExecution.getEndTime());
        log.info("[measure] step={} status={} elapsedMs={}",
                stepExecution.getStepName(), stepExecution.getExitStatus().getExitCode(), elapsed.toMillis());
        if (!stepExecution.getFailureExceptions().isEmpty()) {
            log.warn("[measure] step={} 실패 원인={}", stepExecution.getStepName(),
                    stepExecution.getFailureExceptions().get(0).toString());
        }
        return stepExecution.getExitStatus();
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        Duration elapsed = elapsed(jobExecution.getStartTime(), jobExecution.getEndTime());
        log.info("[measure] job={} topicId={} runDate={} status={} elapsedMs={}",
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getJobParameters().getLong(SelectionJobParameters.TOPIC_ID),
                jobExecution.getJobParameters().getString(SelectionJobParameters.RUN_DATE),
                jobExecution.getStatus(), elapsed.toMillis());
    }

    private static Duration elapsed(LocalDateTime start, LocalDateTime end) {
        return start == null || end == null ? Duration.ZERO : Duration.between(start, end);
    }
}
