package com.siftnews.source.application.service;

import com.siftnews.source.application.port.out.FetchFeedPort;
import com.siftnews.source.application.port.out.LoadActiveSourcesPort;
import com.siftnews.source.application.port.out.SaveArticlePort;
import com.siftnews.source.application.port.out.UpdateSourcePort;
import com.siftnews.source.domain.Article;
import com.siftnews.source.domain.Category;
import com.siftnews.source.domain.RawArticle;
import com.siftnews.source.domain.Source;
import com.siftnews.source.domain.SourceException;
import com.siftnews.source.domain.SourceType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CrawlSourcesServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-07-10T00:00:00Z");

    private final FakeLoadActiveSourcesPort loadActiveSourcesPort = new FakeLoadActiveSourcesPort();
    private final FakeFetchFeedPort fetchFeedPort = new FakeFetchFeedPort();
    private final FakeSaveArticlePort saveArticlePort = new FakeSaveArticlePort();
    private final FakeUpdateSourcePort updateSourcePort = new FakeUpdateSourcePort();
    private final Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    private final CrawlSourcesService service =
            new CrawlSourcesService(loadActiveSourcesPort, fetchFeedPort, saveArticlePort, updateSourcePort, clock);

    @Test
    void crawlAllSavesArticlesAndMarksEachActiveSourceCrawled() {
        Source hackerNews = activeSource(1L, "Hacker News");
        Source techBlog = activeSource(2L, "Tech Blog");
        loadActiveSourcesPort.addAll(hackerNews, techBlog);
        fetchFeedPort.willReturn(1L, List.of(rawArticle("https://hn.example.com/1")));
        fetchFeedPort.willReturn(2L, List.of(rawArticle("https://blog.example.com/1")));

        service.crawlAll();

        assertThat(saveArticlePort.savedArticles())
                .extracting(Article::getUrl)
                .containsExactlyInAnyOrder("https://hn.example.com/1", "https://blog.example.com/1");
        assertThat(hackerNews.getLastCrawledAt()).isEqualTo(FIXED_NOW);
        assertThat(techBlog.getLastCrawledAt()).isEqualTo(FIXED_NOW);
        assertThat(updateSourcePort.markedCrawledAt(1L)).isEqualTo(FIXED_NOW);
        assertThat(updateSourcePort.markedCrawledAt(2L)).isEqualTo(FIXED_NOW);
    }

    @Test
    void crawlAllIsolatesOneSourceFailureFromOthers() {
        Source broken = activeSource(1L, "Broken");
        Source healthy = activeSource(2L, "Healthy");
        loadActiveSourcesPort.addAll(broken, healthy);
        fetchFeedPort.willThrow(1L, new RuntimeException("fetch 실패"));
        fetchFeedPort.willReturn(2L, List.of(rawArticle("https://healthy.example.com/1")));

        service.crawlAll();

        assertThat(broken.getLastCrawledAt()).isNull();
        assertThat(healthy.getLastCrawledAt()).isEqualTo(FIXED_NOW);
        assertThat(saveArticlePort.savedArticles())
                .extracting(Article::getUrl)
                .containsExactly("https://healthy.example.com/1");
        assertThat(updateSourcePort.markedCrawledAt(1L)).isNull();
        assertThat(updateSourcePort.markedCrawledAt(2L)).isEqualTo(FIXED_NOW);
    }

    @Test
    void crawlAllAggregatesMarkCrawledFailuresAndStillProcessesOtherSources() {
        Source broken = activeSource(1L, "Broken");
        Source healthy = activeSource(2L, "Healthy");
        loadActiveSourcesPort.addAll(broken, healthy);
        fetchFeedPort.willReturn(1L, List.of(rawArticle("https://broken.example.com/1")));
        fetchFeedPort.willReturn(2L, List.of(rawArticle("https://healthy.example.com/1")));
        updateSourcePort.willThrow(1L, new RuntimeException("markCrawled 실패"));

        assertThatThrownBy(service::crawlAll)
                .isInstanceOf(SourceException.class)
                .hasMessageContaining("1");

        assertThat(saveArticlePort.savedArticles())
                .extracting(Article::getUrl)
                .containsExactlyInAnyOrder("https://broken.example.com/1", "https://healthy.example.com/1");
        assertThat(healthy.getLastCrawledAt()).isEqualTo(FIXED_NOW);
        assertThat(updateSourcePort.markedCrawledAt(1L)).isNull();
        assertThat(updateSourcePort.markedCrawledAt(2L)).isEqualTo(FIXED_NOW);
    }

    @Test
    void crawlBySourceIdCrawlsOnlyThatSource() {
        Source target = activeSource(1L, "Target");
        Source other = activeSource(2L, "Other");
        loadActiveSourcesPort.addAll(target, other);
        fetchFeedPort.willReturn(1L, List.of(rawArticle("https://target.example.com/1")));

        service.crawl(1L);

        assertThat(target.getLastCrawledAt()).isEqualTo(FIXED_NOW);
        assertThat(other.getLastCrawledAt()).isNull();
        assertThat(updateSourcePort.markedCrawledAt(1L)).isEqualTo(FIXED_NOW);
        assertThat(updateSourcePort.markedCrawledAt(2L)).isNull();
    }

    @Test
    void crawlThrowsSourceExceptionWhenSourceDoesNotExist() {
        assertThatThrownBy(() -> service.crawl(999L))
                .isInstanceOf(SourceException.class)
                .hasMessageContaining("999");
    }

    private static Source activeSource(Long id, String name) {
        return Source.restore(id, name, SourceType.RSS, "https://example.com/rss", "en", Category.DEV, true, null);
    }

    private static RawArticle rawArticle(String url) {
        return new RawArticle(url, "제목", "본문", "en", FIXED_NOW, Category.DEV);
    }

    private static class FakeLoadActiveSourcesPort implements LoadActiveSourcesPort {
        private final List<Source> sources = new ArrayList<>();

        void addAll(Source... toAdd) {
            sources.addAll(List.of(toAdd));
        }

        @Override
        public List<Source> loadActive() {
            return sources;
        }

        @Override
        public Optional<Source> findActiveById(Long sourceId) {
            return sources.stream()
                    .filter(source -> source.getSourceId().equals(sourceId))
                    .findFirst();
        }
    }

    private static class FakeFetchFeedPort implements FetchFeedPort {
        private final Map<Long, List<RawArticle>> articlesBySourceId = new HashMap<>();
        private final Map<Long, RuntimeException> failuresBySourceId = new HashMap<>();

        void willReturn(Long sourceId, List<RawArticle> articles) {
            articlesBySourceId.put(sourceId, articles);
        }

        void willThrow(Long sourceId, RuntimeException exception) {
            failuresBySourceId.put(sourceId, exception);
        }

        @Override
        public List<RawArticle> fetch(Source source) {
            RuntimeException failure = failuresBySourceId.get(source.getSourceId());
            if (failure != null) {
                throw failure;
            }
            return articlesBySourceId.getOrDefault(source.getSourceId(), List.of());
        }
    }

    private static class FakeSaveArticlePort implements SaveArticlePort {
        private final List<Article> saved = new ArrayList<>();

        List<Article> savedArticles() {
            return saved;
        }

        @Override
        public int saveNew(List<Article> articles) {
            saved.addAll(articles);
            return articles.size();
        }
    }

    private static class FakeUpdateSourcePort implements UpdateSourcePort {
        private final Map<Long, Instant> markedAt = new HashMap<>();
        private final Map<Long, RuntimeException> failuresBySourceId = new HashMap<>();

        Instant markedCrawledAt(Long sourceId) {
            return markedAt.get(sourceId);
        }

        void willThrow(Long sourceId, RuntimeException exception) {
            failuresBySourceId.put(sourceId, exception);
        }

        @Override
        public void markCrawled(Long sourceId, Instant at) {
            RuntimeException failure = failuresBySourceId.get(sourceId);
            if (failure != null) {
                throw failure;
            }
            markedAt.put(sourceId, at);
        }
    }
}
