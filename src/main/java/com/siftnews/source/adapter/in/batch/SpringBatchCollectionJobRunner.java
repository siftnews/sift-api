package com.siftnews.source.adapter.in.batch;

import com.siftnews.source.application.port.in.CollectionJobRunSummary;
import com.siftnews.source.application.port.in.CollectionJobRunner;
import com.siftnews.source.domain.SourceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@Profile("!test")
class SpringBatchCollectionJobRunner implements CollectionJobRunner {

    private final JobLauncher jobLauncher;
    private final Job collectionJob;
    private final Clock clock;

    SpringBatchCollectionJobRunner(JobLauncher jobLauncher,
                                   @Qualifier("collectionJob") Job collectionJob,
                                   Clock clock) {
        this.jobLauncher = jobLauncher;
        this.collectionJob = collectionJob;
        this.clock = clock;
    }

    @Override
    public CollectionJobRunSummary run() {
        return run(UUID.randomUUID().toString());
    }

    @Override
    public CollectionJobRunSummary run(String runId) {
        if (runId == null || runId.isBlank()) {
            throw new SourceException("collectionJob runId는 비어 있을 수 없습니다.");
        }
        JobExecution execution;
        try {
            execution = jobLauncher.run(collectionJob, parameters(runId));
        } catch (Exception exception) {
            throw new SourceException("collectionJob 실행에 실패했습니다: runId=" + runId, exception);
        }
        CollectionJobRunSummary summary = summary(runId, execution);
        if (execution.getStatus() == BatchStatus.COMPLETED) {
            log.info("collectionJob 종료: executionId={} status={} exitCode={} sourceRead={} sourceWrite={} sourceSkip={} elapsedMs={}",
                    summary.executionId(), summary.status(), summary.exitCode(), summary.sourceReadCount(),
                    summary.sourceWriteCount(), summary.sourceSkipCount(), summary.elapsedMs());
        } else {
            log.error("collectionJob 실패: executionId={} runId={} status={} exitCode={} sourceRead={} sourceWrite={} sourceSkip={} elapsedMs={} failureDetails={}",
                    summary.executionId(), summary.runId(), summary.status(), summary.exitCode(),
                    summary.sourceReadCount(), summary.sourceWriteCount(), summary.sourceSkipCount(),
                    summary.elapsedMs(), failureDetails(execution));
        }
        return summary;
    }

    private JobParameters parameters(String runId) {
        return new JobParametersBuilder()
                .addLong(CollectionJobParameters.LAUNCHED_AT, clock.millis())
                .addString(CollectionJobParameters.RUN_ID, runId)
                .toJobParameters();
    }

    private static CollectionJobRunSummary summary(String runId, JobExecution execution) {
        StepExecution collectStep = execution.getStepExecutions().stream()
                .filter(step -> "collectStep".equals(step.getStepName()))
                .findFirst()
                .orElse(null);
        int sourceReadCount = collectStep == null ? 0 : Math.toIntExact(collectStep.getReadCount());
        int sourceWriteCount = collectStep == null ? 0 : Math.toIntExact(collectStep.getWriteCount());
        int sourceSkipCount = collectStep == null ? 0 : Math.toIntExact(collectStep.getSkipCount());
        return new CollectionJobRunSummary(
                runId,
                execution.getId(),
                execution.getStatus().name(),
                execution.getExitStatus().getExitCode(),
                sourceReadCount,
                sourceWriteCount,
                sourceSkipCount,
                elapsedMs(execution.getStartTime(), execution.getEndTime()));
    }

    private static long elapsedMs(LocalDateTime start, LocalDateTime end) {
        return start == null || end == null ? 0L : Duration.between(start, end).toMillis();
    }

    private static String failureDetails(JobExecution execution) {
        String exitDescription = sanitize(execution.getExitStatus().getExitDescription());
        String failureTypes = execution.getAllFailureExceptions().stream()
                .map(Throwable::getClass)
                .map(Class::getSimpleName)
                .distinct()
                .collect(Collectors.joining(","));
        return "exitDescription=" + exitDescription + ", failureTypes="
                + (failureTypes.isBlank() ? "none" : failureTypes);
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "none";
        }
        String normalized = value.replaceAll("\\s+", " ");
        return normalized.length() <= 512 ? normalized : normalized.substring(0, 512) + "…";
    }
}
