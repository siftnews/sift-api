package com.siftnews.source.adapter.in.batch;

import com.siftnews.source.application.port.out.FetchFeedPort;
import com.siftnews.source.application.port.out.LoadActiveSourcesPort;
import com.siftnews.source.application.port.out.SaveArticlePort;
import com.siftnews.source.domain.Article;
import com.siftnews.source.domain.Category;
import com.siftnews.source.domain.RawArticle;
import com.siftnews.source.domain.Source;
import com.siftnews.source.domain.SourceType;
import com.siftnews.support.AbstractIntegrationTest;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * collectionJob 조립 통합 테스트 — {@code @SpringBatchTest}로 Job을 실제 기동한다.
 * out-port는 {@code @Primary} fake로 오버라이드해 RSS 네트워크/DB 적재 없이 배선만 검증한다.
 * (JobRepository 메타테이블은 Testcontainers Postgres 위에서 동작)
 */
@SpringBatchTest
class CollectionJobIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    /** selectionJob이 생기며 Job 빈이 둘이 되어, 어느 것을 돌릴지 명시해야 한다 (이슈 #31). */
    @Autowired
    @Qualifier("collectionJob")
    private Job collectionJob;

    @Autowired
    private FakeSaveArticlePort saveArticlePort;

    @Autowired
    private FakeFetchFeedPort fetchFeedPort;

    @Autowired
    private MeterRegistry meterRegistry;

    @BeforeEach
    void reset() {
        jobLauncherTestUtils.setJob(collectionJob);
        saveArticlePort.clear();
        fetchFeedPort.clearFailures();
    }

    @Test
    void runsCollectStepAndSavesArticlesFromAllActiveSources() throws Exception {
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getStepName()).isEqualTo("collectStep");
        assertThat(stepExecution.getReadCount()).isEqualTo(2); // 활성 소스 2건

        assertThat(saveArticlePort.savedArticles())
                .extracting(Article::getUrl)
                .containsExactlyInAnyOrder(
                        "https://hn.example.com/1",
                        "https://hn.example.com/2",
                        "https://blog.example.com/1");
    }

    @Test
    void skipsFailingSourceAndStillCompletesJob() throws Exception {
        fetchFeedPort.willThrow(2L); // 소스 2가 fetch 중 예외 → 소스 1만 살아남아야 한다

        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        // 한 소스의 오류가 Job 전체를 실패시키지 않고 해당 소스만 skip 격리된다
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getSkipCount()).isEqualTo(1);
        assertThat(saveArticlePort.savedArticles())
                .extracting(Article::getUrl)
                .containsExactlyInAnyOrder(
                        "https://hn.example.com/1",
                        "https://hn.example.com/2");
    }

    @Test
    void publishesBatchMetricsToMicrometerRegistry() throws Exception {
        jobLauncherTestUtils.launchJob();

        // Spring Batch 5는 job/step 소요시간을 Observation API로 기록하고, 부트가 이를
        // MeterRegistry에 바인딩한다. 액추에이터가 이 레지스트리를 /actuator/metrics 로 노출한다.
        Timer jobTimer = meterRegistry.find("spring.batch.job")
                .tag("spring.batch.job.name", "collectionJob")
                .tag("spring.batch.job.status", "COMPLETED")
                .timer();
        Timer stepTimer = meterRegistry.find("spring.batch.step")
                .tag("spring.batch.step.name", "collectStep")
                .tag("spring.batch.step.job.name", "collectionJob")
                .tag("spring.batch.step.status", "COMPLETED")
                .timer();

        assertThat(jobTimer).isNotNull();
        assertThat(stepTimer).isNotNull();
        assertThat(meterRegistry.find("spring.batch.job.active").longTaskTimer())
                .isNotNull()
                .satisfies(activeTimer -> assertThat(activeTimer.getId().getTag("spring.batch.job.active.name"))
                        .isEqualTo("collectionJob"));
    }

    @Test
    void allowsRelaunchOnlyWhenLaunchedAtDiffers() throws Exception {
        JobParameters firstCycle = launchedAt(1_000L);
        JobParameters secondCycle = launchedAt(2_000L);

        assertThat(jobLauncherTestUtils.launchJob(firstCycle).getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobLauncherTestUtils.launchJob(secondCycle).getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // 파라미터가 같으면 같은 JobInstance라 재기동이 거부된다 — collectionTrigger가 매 주기
        // launchedAt을 새로 넣는 이유이자, 넣지 않으면 두 번째 주기부터 수집이 멈추는 회귀의 방어선.
        assertThatThrownBy(() -> jobLauncherTestUtils.launchJob(firstCycle))
                .isInstanceOf(JobInstanceAlreadyCompleteException.class);
    }

    private static JobParameters launchedAt(long epochMilli) {
        return new JobParametersBuilder()
                .addLong(CollectionJobParameters.LAUNCHED_AT, epochMilli)
                .toJobParameters();
    }

    @TestConfiguration
    static class FakePortsConfig {

        @Bean
        @Primary
        LoadActiveSourcesPort fakeLoadActiveSourcesPort() {
            return new FakeLoadActiveSourcesPort(
                    activeSource(1L, "Hacker News"),
                    activeSource(2L, "Tech Blog"));
        }

        @Bean
        @Primary
        FakeFetchFeedPort fakeFetchFeedPort() {
            FakeFetchFeedPort port = new FakeFetchFeedPort();
            port.willReturn(1L, List.of(rawArticle("https://hn.example.com/1"), rawArticle("https://hn.example.com/2")));
            port.willReturn(2L, List.of(rawArticle("https://blog.example.com/1")));
            return port;
        }

        @Bean
        @Primary
        FakeSaveArticlePort fakeSaveArticlePort() {
            return new FakeSaveArticlePort();
        }
    }

    private static Source activeSource(Long id, String name) {
        return Source.restore(id, name, SourceType.RSS, "https://example.com/rss", "en", Category.DEV, true, null);
    }

    private static RawArticle rawArticle(String url) {
        return new RawArticle(url, "제목", "본문", "en", Instant.parse("2026-07-10T00:00:00Z"), Category.DEV);
    }

    static class FakeLoadActiveSourcesPort implements LoadActiveSourcesPort {
        private final List<Source> sources;

        FakeLoadActiveSourcesPort(Source... sources) {
            this.sources = List.of(sources);
        }

        @Override
        public List<Source> loadActive() {
            return sources;
        }

        @Override
        public Optional<Source> findActiveById(Long sourceId) {
            return sources.stream().filter(s -> s.getSourceId().equals(sourceId)).findFirst();
        }
    }

    static class FakeFetchFeedPort implements FetchFeedPort {
        private final Map<Long, List<RawArticle>> bySourceId = new HashMap<>();
        private final Set<Long> failingSourceIds = new HashSet<>();

        void willReturn(Long sourceId, List<RawArticle> articles) {
            bySourceId.put(sourceId, articles);
        }

        /** 해당 소스 fetch 시 예외를 던지게 한다(내결함성 검증용). */
        void willThrow(Long sourceId) {
            failingSourceIds.add(sourceId);
        }

        void clearFailures() {
            failingSourceIds.clear();
        }

        @Override
        public List<RawArticle> fetch(Source source) {
            if (failingSourceIds.contains(source.getSourceId())) {
                throw new IllegalStateException("fetch 실패(테스트): sourceId=" + source.getSourceId());
            }
            return bySourceId.getOrDefault(source.getSourceId(), List.of());
        }
    }

    static class FakeSaveArticlePort implements SaveArticlePort {
        private final List<Article> saved = new ArrayList<>();

        List<Article> savedArticles() {
            return saved;
        }

        void clear() {
            saved.clear();
        }

        @Override
        public int saveNew(List<Article> articles) {
            // 실제 UNIQUE(normalized_url) 중복 무시를 모사 — skip 재처리 시 중복 저장 방지
            int before = saved.size();
            for (Article article : articles) {
                boolean exists = saved.stream().anyMatch(s -> s.getUrl().equals(article.getUrl()));
                if (!exists) {
                    saved.add(article);
                }
            }
            return saved.size() - before;
        }
    }
}
