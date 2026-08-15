package com.siftnews.source.adapter.out.loadtest;

import com.siftnews.source.domain.Category;
import com.siftnews.source.domain.Source;
import com.siftnews.source.domain.SourceType;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LoadtestFetchFeedAdapterTest {

    @Test
    void createsDeterministicUniqueArticlesPerSyntheticSource() {
        Instant now = Instant.parse("2026-08-16T00:00:00Z");
        LoadtestFetchFeedAdapter adapter = new LoadtestFetchFeedAdapter(
                Clock.fixed(now, ZoneOffset.UTC), 3, "https://fixture/articles/");
        Source source = Source.restore(101L, "Loadtest source 02", SourceType.RSS,
                "https://loadtest.sift.local/news/source-02.xml", "en", Category.DEV, true, null);

        var first = adapter.fetch(source);
        var second = adapter.fetch(source);

        assertThat(first).hasSize(3).extracting(article -> article.url())
                .containsExactly(
                        "https://fixture/articles/direct/source-02/article-0000",
                        "https://fixture/articles/direct/source-02/article-0001",
                        "https://fixture/articles/direct/source-02/article-0002");
        assertThat(first).isEqualTo(second);
        assertThat(first).allSatisfy(article -> {
            assertThat(article.title()).contains("Spring");
            assertThat(article.body()).hasSizeGreaterThanOrEqualTo(200);
            assertThat(article.publishedAt()).isEqualTo(now.minusSeconds(1));
        });
    }

    @Test
    void usesTheBatchRunIdToSeparateCollectionArticleNamespaces() {
        LoadtestFetchFeedAdapter adapter = new LoadtestFetchFeedAdapter(
                Clock.fixed(Instant.parse("2026-08-16T00:00:00Z"), ZoneOffset.UTC),
                1, "https://fixture/articles/");
        Source source = Source.restore(101L, "Loadtest source 02", SourceType.RSS,
                "https://loadtest.sift.local/news/source-02.xml", "en", Category.DEV, true, null);
        JobExecution execution = new JobExecution(1L, new JobParametersBuilder()
                .addString("runId", "collection-valid-1")
                .toJobParameters());
        StepSynchronizationManager.register(new StepExecution("collectStep", execution));

        try {
            assertThat(adapter.fetch(source).get(0).url())
                    .isEqualTo("https://fixture/articles/collection-valid-1/source-02/article-0000");
        } finally {
            StepSynchronizationManager.close();
        }
    }
}
