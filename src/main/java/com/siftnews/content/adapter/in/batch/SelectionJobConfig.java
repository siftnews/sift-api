package com.siftnews.content.adapter.in.batch;

import com.siftnews.content.application.port.in.BuildIssueUseCase;
import com.siftnews.content.application.port.in.NormalizeDedupUseCase;
import com.siftnews.content.application.port.in.ScoreArticlesUseCase;
import org.springframework.batch.core.Job;
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

import java.time.Instant;
import java.time.LocalDate;

/**
 * selectionJob 조립 — 인바운드 어댑터 (MVP-DESIGN §3③). 도메인/애플리케이션은 배치를 모른다.
 * <p>
 * 세 Step 모두 <b>tasklet</b>이다. 설계 초안은 {@code scoreStep (chunk)}였지만, 선별은
 * "윈도우 후보 전체를 다시 계산해 상태를 통째로 교체"하는 것이 멱등성의 전제라(D-031)
 * 아이템 단위로 쪼갤 수 없다. 후보량이 커져 성능 로드맵에 오르면 그때 재검토한다.
 * <p>
 * 윈도우 {@code [from, to)}는 <b>트리거가 계산해 파라미터로 주입</b>한다 — Step이 스스로
 * "지금"을 기준으로 잡으면 토픽마다 값이 달라져 D-032 불변식 (3)이 깨진다.
 */
@Configuration
class SelectionJobConfig {

    @Bean
    Job selectionJob(JobRepository jobRepository, Step normalizeDedupStep, Step scoreStep, Step selectStep,
                     SelectionMetricsListener metricsListener) {
        return new JobBuilder("selectionJob", jobRepository)
                .listener(metricsListener)
                .start(normalizeDedupStep)
                .next(scoreStep)
                .next(selectStep)
                .build();
    }

    @Bean
    Step normalizeDedupStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                            Tasklet normalizeDedupTasklet, SelectionMetricsListener metricsListener) {
        return new StepBuilder("normalizeDedupStep", jobRepository)
                .tasklet(normalizeDedupTasklet, transactionManager)
                .listener(metricsListener)
                .build();
    }

    @Bean
    Step scoreStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                   Tasklet scoreTasklet, SelectionMetricsListener metricsListener) {
        return new StepBuilder("scoreStep", jobRepository)
                .tasklet(scoreTasklet, transactionManager)
                .listener(metricsListener)
                .build();
    }

    @Bean
    Step selectStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                    Tasklet selectTasklet, SelectionMetricsListener metricsListener) {
        return new StepBuilder("selectStep", jobRepository)
                .tasklet(selectTasklet, transactionManager)
                .listener(metricsListener)
                .build();
    }

    /**
     * 토픽 독립 전역 단계인데 selectionJob은 토픽마다 기동되므로 <b>토픽 수만큼 반복</b>된다.
     * 윈도우가 고정돼 있고 재실행이 멱등이라(D-031) 결과는 같다 — 중복 실행 비용이 측정으로
     * 드러나면 전역 Job으로 분리한다.
     */
    @Bean
    @StepScope
    Tasklet normalizeDedupTasklet(NormalizeDedupUseCase normalizeDedupUseCase,
                                  @Value("#{jobParameters['" + SelectionJobParameters.WINDOW_FROM + "']}") String from,
                                  @Value("#{jobParameters['" + SelectionJobParameters.WINDOW_TO + "']}") String to) {
        return (contribution, chunkContext) -> {
            normalizeDedupUseCase.normalizeAndDedup(Instant.parse(from), Instant.parse(to));
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    @StepScope
    Tasklet scoreTasklet(ScoreArticlesUseCase scoreArticlesUseCase,
                         @Value("#{jobParameters['" + SelectionJobParameters.TOPIC_ID + "']}") Long topicId,
                         @Value("#{jobParameters['" + SelectionJobParameters.WINDOW_FROM + "']}") String from,
                         @Value("#{jobParameters['" + SelectionJobParameters.WINDOW_TO + "']}") String to) {
        return (contribution, chunkContext) -> {
            scoreArticlesUseCase.scoreTopic(topicId, Instant.parse(from), Instant.parse(to));
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    @StepScope
    Tasklet selectTasklet(BuildIssueUseCase buildIssueUseCase,
                          @Value("#{jobParameters['" + SelectionJobParameters.TOPIC_ID + "']}") Long topicId,
                          @Value("#{jobParameters['" + SelectionJobParameters.RUN_DATE + "']}") String runDate) {
        return (contribution, chunkContext) -> {
            buildIssueUseCase.buildIssueForTopic(topicId, LocalDate.parse(runDate));
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    SelectionMetricsListener selectionMetricsListener() {
        return new SelectionMetricsListener();
    }
}
