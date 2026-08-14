package com.siftnews.delivery.adapter.in.batch;

import com.siftnews.delivery.application.port.in.DispatchIssueUseCase;
import com.siftnews.delivery.application.port.in.SendDeliveryTaskUseCase;
import com.siftnews.delivery.application.port.out.LoadDeliveryJobPort;
import com.siftnews.delivery.application.port.out.LoadPendingDeliveryTasksPort;
import com.siftnews.delivery.domain.DeliveryTask;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/** dispatchJob 조립 — 스냅샷 생성 뒤 PENDING task를 고정 HTML 이메일로 발송한다. */
@Configuration
class DispatchJobConfig {

    @Bean
    Job dispatchJob(JobRepository jobRepository, Step snapshotStep, Step sendStep,
                    JobParametersValidator dispatchJobParametersValidator,
                    DispatchMetricsListener metricsListener,
                    DeliveryJobLifecycleListener lifecycleListener) {
        return new JobBuilder("dispatchJob", jobRepository)
                .validator(dispatchJobParametersValidator)
                .listener(metricsListener)
                .listener(lifecycleListener)
                .start(snapshotStep)
                .next(sendStep)
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
    Step sendStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                  @Qualifier("pendingDeliveryTaskReader") ItemStreamReader<DeliveryTask> pendingDeliveryTaskReader,
                  @Qualifier("deliveryEmailWriter") ItemWriter<DeliveryTask> deliveryEmailWriter,
                  DispatchMetricsListener metricsListener,
                  DeliveryJobLifecycleListener lifecycleListener) {
        return new StepBuilder("sendStep", jobRepository)
                .<DeliveryTask, DeliveryTask>chunk(500, transactionManager)
                .reader(pendingDeliveryTaskReader)
                .writer(deliveryEmailWriter)
                .listener(metricsListener)
                .listener(lifecycleListener)
                .build();
    }

    @Bean
    @StepScope
    ItemStreamReader<DeliveryTask> pendingDeliveryTaskReader(LoadDeliveryJobPort loadDeliveryJobPort,
            LoadPendingDeliveryTasksPort loadPendingDeliveryTasksPort,
            @Value("#{jobParameters['" + DispatchJobParameters.ISSUE_ID + "']}") Long issueId) {
        return new PendingDeliveryTaskReader(issueId, loadDeliveryJobPort, loadPendingDeliveryTasksPort);
    }

    @Bean
    @StepScope
    ItemWriter<DeliveryTask> deliveryEmailWriter(SendDeliveryTaskUseCase sendDeliveryTaskUseCase) {
        return new DeliveryEmailWriter(sendDeliveryTaskUseCase);
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
