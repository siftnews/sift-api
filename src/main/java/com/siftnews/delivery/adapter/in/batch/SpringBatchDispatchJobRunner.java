package com.siftnews.delivery.adapter.in.batch;

import com.siftnews.delivery.application.port.in.DispatchJobRunSummary;
import com.siftnews.delivery.application.port.in.DispatchJobRunner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

@Slf4j
@Component
class SpringBatchDispatchJobRunner implements DispatchJobRunner {

    private final JobLauncher jobLauncher;
    private final Job dispatchJob;
    private final Clock clock;

    SpringBatchDispatchJobRunner(JobLauncher jobLauncher,
                                 @Qualifier("dispatchJob") Job dispatchJob,
                                 Clock clock) {
        this.jobLauncher = jobLauncher;
        this.dispatchJob = dispatchJob;
        this.clock = clock;
    }

    @Override
    public DispatchJobRunSummary run(Long issueId, Long topicId, int preferredSendHour) throws Exception {
        JobExecution execution = jobLauncher.run(dispatchJob, parameters(issueId, topicId, preferredSendHour));
        DispatchJobRunSummary summary = summary(execution);
        log.info("dispatchJob 종료: executionId={} status={} exitCode={} createdTasks={} processedTasks={} failedTasks={}",
                summary.executionId(), summary.status(), summary.exitCode(), summary.createdTaskCount(),
                summary.processedTaskCount(), summary.failedTaskCount());
        return summary;
    }

    private JobParameters parameters(Long issueId, Long topicId, int preferredSendHour) {
        return new JobParametersBuilder()
                .addLong(DispatchJobParameters.ISSUE_ID, issueId)
                .addLong(DispatchJobParameters.TOPIC_ID, topicId)
                .addLong(DispatchJobParameters.SEND_HOUR, (long) preferredSendHour)
                .addLong(DispatchJobParameters.LAUNCHED_AT, clock.millis())
                // 같은 밀리초에 들어온 수동 요청도 서로 다른 JobInstance로 만든다.
                .addString(DispatchJobParameters.RUN_ID, UUID.randomUUID().toString())
                .toJobParameters();
    }

    private static DispatchJobRunSummary summary(JobExecution execution) {
        return new DispatchJobRunSummary(
                execution.getId(),
                execution.getStatus().name(),
                execution.getExitStatus().getExitCode(),
                executionContextCount(execution, "snapshotStep", DispatchMetricsListener.CREATED_TASK_COUNT),
                writeCount(execution, "sendStep"),
                executionContextCount(execution, "sendStep", DeliveryEmailWriter.FAILED_TASK_COUNT));
    }

    private static int executionContextCount(JobExecution execution, String stepName, String key) {
        return execution.getStepExecutions().stream()
                .filter(stepExecution -> stepName.equals(stepExecution.getStepName()))
                .mapToInt(stepExecution -> stepExecution.getExecutionContext().getInt(key, 0))
                .sum();
    }

    private static int writeCount(JobExecution execution, String stepName) {
        return Math.toIntExact(execution.getStepExecutions().stream()
                .filter(stepExecution -> stepName.equals(stepExecution.getStepName()))
                .mapToLong(StepExecution::getWriteCount)
                .sum());
    }
}
