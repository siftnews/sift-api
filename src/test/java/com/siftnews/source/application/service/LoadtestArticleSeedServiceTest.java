package com.siftnews.source.application.service;

import com.siftnews.source.application.port.in.LoadtestArticleSeedSummary;
import com.siftnews.source.application.port.out.LoadActiveSourcesPort;
import com.siftnews.source.application.port.out.SaveArticlePort;
import com.siftnews.source.domain.Article;
import com.siftnews.source.domain.Category;
import com.siftnews.source.domain.Source;
import com.siftnews.source.domain.SourceType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoadtestArticleSeedServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-16T00:00:00Z");

    @Test
    void seedsTheConfiguredCandidateCountAcrossSyntheticSources() {
        RecordingSaveArticlePort saveArticlePort = new RecordingSaveArticlePort();
        LoadtestArticleSeedService service = new LoadtestArticleSeedService(
                new FakeSources(activeSource(1L, "00"), activeSource(2L, "01")), saveArticlePort,
                Clock.fixed(NOW, ZoneOffset.UTC), 2, 6, 24, 2,
                "https://fixture/selection/");

        LoadtestArticleSeedSummary summary = service.seed("run-1");

        assertThat(summary).isEqualTo(new LoadtestArticleSeedSummary(
                "run-1", 6, 6, 2, NOW, NOW.plusSeconds(24 * 60 * 60L)));
        assertThat(saveArticlePort.saved).hasSize(6)
                .extracting(Article::getUrl)
                .allMatch(url -> url.contains("/run-1/"));
        assertThat(saveArticlePort.calls).containsExactly(2, 1, 2, 1);
        assertThat(saveArticlePort.saved).extracting(Article::getSourceId)
                .containsExactly(1L, 1L, 1L, 2L, 2L, 2L);
    }

    @Test
    void rejectsAnUnsafeRunIdBeforeWriting() {
        RecordingSaveArticlePort saveArticlePort = new RecordingSaveArticlePort();
        LoadtestArticleSeedService service = new LoadtestArticleSeedService(
                new FakeSources(activeSource(1L, "00")), saveArticlePort,
                Clock.fixed(NOW, ZoneOffset.UTC), 1, 1, 24, 1,
                "https://fixture/selection/");

        assertThatThrownBy(() -> service.seed("../unsafe"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(saveArticlePort.saved).isEmpty();
    }

    private static Source activeSource(Long id, String key) {
        return Source.restore(id, "Loadtest source " + key, SourceType.RSS,
                "https://loadtest.sift.local/news/source-" + key + ".xml", "en", Category.DEV, true, null);
    }

    private record FakeSources(List<Source> sources) implements LoadActiveSourcesPort {
        private FakeSources(Source... sources) {
            this(List.of(sources));
        }

        @Override
        public List<Source> loadActive() {
            return sources;
        }

        @Override
        public Optional<Source> findActiveById(Long sourceId) {
            return sources.stream().filter(source -> source.getSourceId().equals(sourceId)).findFirst();
        }
    }

    private static final class RecordingSaveArticlePort implements SaveArticlePort {
        private final List<Article> saved = new ArrayList<>();
        private final List<Integer> calls = new ArrayList<>();

        @Override
        public int saveNew(List<Article> articles) {
            calls.add(articles.size());
            saved.addAll(articles);
            return articles.size();
        }
    }
}
