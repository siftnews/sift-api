package com.siftnews.delivery.adapter.in.batch;

import com.siftnews.delivery.application.port.out.LoadRetriableDeliveryTasksPort;
import com.siftnews.delivery.application.service.DeliveryRetryPolicy;
import com.siftnews.delivery.domain.DeliveryTask;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;
import java.time.Duration;

@Configuration
class RetryJobConfig {

    @Bean
    Job retryJob(JobRepository jobRepository, Step retryStep, RetryMetricsListener retryMetricsListener,
                 DeliveryJobLifecycleListener lifecycleListener) {
        return new JobBuilder("retryJob", jobRepository)
                .listener(retryMetricsListener)
                .listener(lifecycleListener)
                .start(retryStep)
                .build();
    }

    @Bean
    Step retryStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                   @Qualifier("retryDeliveryTaskReader") ItemStreamReader<DeliveryTask> reader,
                   @Qualifier("deliveryEmailWriter") ItemWriter<DeliveryTask> writer,
                   RetryMetricsListener retryMetricsListener) {
        return new StepBuilder("retryStep", jobRepository)
                .<DeliveryTask, DeliveryTask>chunk(500, transactionManager)
                .reader(reader)
                .writer(writer)
                .listener(retryMetricsListener)
                .build();
    }

    @Bean
    @StepScope
    ItemStreamReader<DeliveryTask> retryDeliveryTaskReader(
            LoadRetriableDeliveryTasksPort loadRetriableDeliveryTasksPort,
            Clock clock,
            DeliveryRetryPolicy deliveryRetryPolicy) {
        return new RetryDeliveryTaskReader(loadRetriableDeliveryTasksPort, clock,
                deliveryRetryPolicy.maxAttempts());
    }

    @Bean
    DeliveryRetryPolicy deliveryRetryPolicy(
            @Value("${sift.delivery.retry.max-attempts:3}") int maxAttempts,
            @Value("${sift.delivery.retry.base-delay-seconds:60}") long baseDelaySeconds,
            @Value("${sift.delivery.retry.max-delay-seconds:3600}") long maxDelaySeconds) {
        return new DeliveryRetryPolicy(maxAttempts, Duration.ofSeconds(baseDelaySeconds),
                Duration.ofSeconds(maxDelaySeconds));
    }

    @Bean
    RetryMetricsListener retryMetricsListener(MeterRegistry meterRegistry) {
        return new RetryMetricsListener(meterRegistry);
    }
}
