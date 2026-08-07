package com.siftnews.delivery.adapter.in.batch;

import com.siftnews.delivery.application.port.in.DispatchIssueUseCase;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/** dispatchJob 조립 — 스냅샷 생성만 담당하며 실제 발송·재시도 Step은 후속 이슈에서 추가한다. */
@Configuration
class DispatchJobConfig {

    @Bean
    Job dispatchJob(JobRepository jobRepository, Step snapshotStep, JobParametersValidator dispatchJobParametersValidator,
                    DispatchMetricsListener metricsListener) {
        return new JobBuilder("dispatchJob", jobRepository)
                .validator(dispatchJobParametersValidator)
                .listener(metricsListener)
                .start(snapshotStep)
                .build();
    }

    @Bean
    Step snapshotStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                      Tasklet snapshotTasklet, DispatchMetricsListener metricsListener) {
        return new StepBuilder("snapshotStep", jobRepository)
                .tasklet(snapshotTasklet, transactionManager)
                .listener(metricsListener)
                .build();
    }

    @Bean
    @StepScope
    Tasklet snapshotTasklet(DispatchIssueUseCase dispatchIssueUseCase,
                            @Value("#{jobParameters['" + DispatchJobParameters.ISSUE_ID + "']}") Long issueId,
                            @Value("#{jobParameters['" + DispatchJobParameters.TOPIC_ID + "']}") Long topicId,
                            @Value("#{jobParameters['" + DispatchJobParameters.SEND_HOUR + "']}") Long sendHour) {
        return (contribution, chunkContext) -> {
            var summary = dispatchIssueUseCase.dispatch(issueId, topicId, sendHour.intValue());
            contribution.getStepExecution().getExecutionContext()
                    .putInt(DispatchMetricsListener.CREATED_TASK_COUNT, summary.createdTaskCount());
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    JobParametersValidator dispatchJobParametersValidator() {
        return new DispatchJobParametersValidator();
    }

    @Bean
    DispatchMetricsListener dispatchMetricsListener(io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        return new DispatchMetricsListener(meterRegistry);
    }
}
