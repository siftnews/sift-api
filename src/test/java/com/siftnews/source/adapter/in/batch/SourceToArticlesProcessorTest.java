package com.siftnews.source.adapter.in.batch;

import com.siftnews.source.application.port.out.FetchFeedPort;
import com.siftnews.source.domain.Article;
import com.siftnews.source.domain.Category;
import com.siftnews.source.domain.RawArticle;
import com.siftnews.source.domain.Source;
import com.siftnews.source.domain.SourceType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SourceToArticlesProcessorTest {

    private static final Instant NOW = Instant.parse("2026-07-10T00:00:00Z");

    @Test
    void convertsEachRawArticleToAnArticleStampedWithSourceId() {
        Source source = activeSource(7L);
        FakeFetchFeedPort fetchFeedPort = new FakeFetchFeedPort();
        fetchFeedPort.willReturn(7L, List.of(rawArticle("https://example.com/a"), rawArticle("https://example.com/b")));
        SourceToArticlesProcessor processor = new SourceToArticlesProcessor(fetchFeedPort);

        List<Article> articles = processor.process(source);

        assertThat(articles)
                .extracting(Article::getUrl)
                .containsExactly("https://example.com/a", "https://example.com/b");
        assertThat(articles).allMatch(a -> a.getSourceId().equals(7L));
    }

    @Test
    void returnsEmptyListWhenFeedHasNoArticles() {
        Source source = activeSource(7L);
        SourceToArticlesProcessor processor = new SourceToArticlesProcessor(new FakeFetchFeedPort());

        assertThat(processor.process(source)).isEmpty();
    }

    private static Source activeSource(Long id) {
        return Source.restore(id, "Feed", SourceType.RSS, "https://example.com/rss", "en", Category.DEV, true, null);
    }

    private static RawArticle rawArticle(String url) {
        return new RawArticle(url, "제목", "본문", "en", NOW, Category.DEV);
    }

    private static class FakeFetchFeedPort implements FetchFeedPort {
        private final Map<Long, List<RawArticle>> bySourceId = new HashMap<>();

        void willReturn(Long sourceId, List<RawArticle> articles) {
            bySourceId.put(sourceId, articles);
        }

        @Override
        public List<RawArticle> fetch(Source source) {
            return bySourceId.getOrDefault(source.getSourceId(), List.of());
        }
    }
}
