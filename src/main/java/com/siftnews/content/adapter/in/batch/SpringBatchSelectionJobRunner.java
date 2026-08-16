package com.siftnews.content.adapter.in.batch;

import com.siftnews.content.application.port.in.SelectionJobRunSummary;
import com.siftnews.content.application.port.in.SelectionJobRunner;
import com.siftnews.content.domain.ContentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@Profile("!test")
class SpringBatchSelectionJobRunner implements SelectionJobRunner {

    private final JobLauncher jobLauncher;
    private final Job selectionJob;
    private final Clock clock;

    SpringBatchSelectionJobRunner(JobLauncher jobLauncher,
                                  @Qualifier("selectionJob") Job selectionJob,
                                  Clock clock) {
        this.jobLauncher = jobLauncher;
        this.selectionJob = selectionJob;
        this.clock = clock;
    }

    @Override
    public SelectionJobRunSummary run(Long topicId, LocalDate runDate, Instant from, Instant to) {
        if (topicId == null || runDate == null || from == null || to == null || !from.isBefore(to)) {
            throw new ContentException("selectionJob 윈도우 파라미터가 유효하지 않습니다.");
        }

        JobExecution execution;
        try {
            execution = jobLauncher.run(selectionJob, parameters(topicId, runDate, from, to));
        } catch (Exception exception) {
            throw new ContentException("selectionJob 실행에 실패했습니다.", exception);
        }
        SelectionJobRunSummary summary = new SelectionJobRunSummary(
                execution.getId(), topicId, runDate, from, to,
                execution.getStatus().name(), execution.getExitStatus().getExitCode(),
                elapsedMs(execution.getStartTime(), execution.getEndTime()));
        if (execution.getStatus() == BatchStatus.COMPLETED) {
            log.info("selectionJob 종료: executionId={} topicId={} runDate={} status={} exitCode={} window=[{}, {}) elapsedMs={}",
                    summary.executionId(), summary.topicId(), summary.runDate(), summary.status(), summary.exitCode(),
                    summary.windowFrom(), summary.windowTo(), summary.elapsedMs());
        } else {
            log.error("selectionJob 실패: executionId={} topicId={} runDate={} status={} exitCode={} window=[{}, {}) elapsedMs={} failureDetails={}",
                    summary.executionId(), summary.topicId(), summary.runDate(), summary.status(), summary.exitCode(),
                    summary.windowFrom(), summary.windowTo(), summary.elapsedMs(), failureDetails(execution));
        }
        return summary;
    }

    private JobParameters parameters(Long topicId, LocalDate runDate, Instant from, Instant to) {
        return new JobParametersBuilder()
                .addLong(SelectionJobParameters.TOPIC_ID, topicId)
                .addString(SelectionJobParameters.RUN_DATE, runDate.toString())
                .addString(SelectionJobParameters.WINDOW_FROM, from.toString())
                .addString(SelectionJobParameters.WINDOW_TO, to.toString())
                .addLong(SelectionJobParameters.LAUNCHED_AT, clock.millis())
                // 같은 밀리초에 들어온 수동 요청도 서로 다른 JobInstance로 만든다.
                .addString(SelectionJobParameters.RUN_ID, UUID.randomUUID().toString())
                .toJobParameters();
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
