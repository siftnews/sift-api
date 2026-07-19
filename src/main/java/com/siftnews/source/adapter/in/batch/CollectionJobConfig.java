package com.siftnews.source.adapter.in.batch;

import com.siftnews.source.application.port.out.FetchFeedPort;
import com.siftnews.source.application.port.out.LoadActiveSourcesPort;
import com.siftnews.source.application.port.out.SaveArticlePort;
import com.siftnews.source.domain.Article;
import com.siftnews.source.domain.Source;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

/**
 * collectionJob 조립 — 인바운드 어댑터(Spring Batch Job). 도메인/애플리케이션은 배치를 모른다.
 * <p>
 * 단일 {@code collectStep}(chunk=50): 활성 소스를 읽어(reader) 피드를 기사 목록으로 변환하고
 * (processor), chunk를 평탄화해 저장한다(writer). reader/processor/writer는 out-port에 직접
 * 배선한다({@code CrawlSourcesService}를 거치지 않음 — MVP-DESIGN §3①).
 */
@Configuration
class CollectionJobConfig {

    /** 아이템 = 소스 하나이므로 chunk 50 = 소스 50개 단위 트랜잭션. */
    private static final int CHUNK_SIZE = 50;

    /**
     * 소스별 오류 격리 한계 — 한 소스의 fetch/변환 실패가 전체 수집 Job을 실패시키지 않도록
     * skip으로 넘긴다. 이 수를 넘는 대량 실패는 계통 장애로 보고 step을 실패시켜 가시화한다.
     * (TRANSIENT/PERMANENT 분류·재시도 백오프는 후속 — 소스 재시도 상태 스키마 필요)
     */
    private static final int SKIP_LIMIT = 100;

    @Bean
    Job collectionJob(JobRepository jobRepository, Step collectStep, CollectionMetricsListener metricsListener) {
        return new JobBuilder("collectionJob", jobRepository)
                .listener(metricsListener)
                .start(collectStep)
                .build();
    }

    @Bean
    Step collectStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ActiveSourceItemReader activeSourceItemReader,
            SourceToArticlesProcessor sourceToArticlesProcessor,
            ArticleChunkWriter articleChunkWriter,
            CollectionMetricsListener metricsListener) {
        // TODO(#14 후속): 배치가 3개 out-port에 직접 배선되어 오케스트레이션이 CrawlSourcesService와
        //  이원화됨(MVP-DESIGN §3① 의도적 결정). 크롤링 정책이 늘면 UseCase(application.service)로
        //  오케스트레이션 이관 검토 — chunk 모델상 item 단위 reader/processor/writer는 유지.
        return new StepBuilder("collectStep", jobRepository)
                .<Source, List<Article>>chunk(CHUNK_SIZE, transactionManager)
                .reader(activeSourceItemReader)
                .processor(sourceToArticlesProcessor)
                .writer(articleChunkWriter)
                .listener(metricsListener)
                // 소스별 오류 격리: 한 소스 실패가 전체 Job을 죽이지 않도록 skip (비배치 CrawlSourcesService와 동일 회복력)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(SKIP_LIMIT)
                .build();
    }

    @Bean
    @StepScope
    ActiveSourceItemReader activeSourceItemReader(LoadActiveSourcesPort loadActiveSourcesPort) {
        return new ActiveSourceItemReader(loadActiveSourcesPort);
    }

    @Bean
    SourceToArticlesProcessor sourceToArticlesProcessor(FetchFeedPort fetchFeedPort) {
        return new SourceToArticlesProcessor(fetchFeedPort);
    }

    @Bean
    ArticleChunkWriter articleChunkWriter(SaveArticlePort saveArticlePort) {
        return new ArticleChunkWriter(saveArticlePort);
    }

    @Bean
    CollectionMetricsListener collectionMetricsListener() {
        return new CollectionMetricsListener();
    }
}
