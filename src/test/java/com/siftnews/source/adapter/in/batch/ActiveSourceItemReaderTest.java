package com.siftnews.source.adapter.in.batch;

import com.siftnews.source.application.port.out.LoadActiveSourcesPort;
import com.siftnews.source.domain.Category;
import com.siftnews.source.domain.Source;
import com.siftnews.source.domain.SourceType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ActiveSourceItemReaderTest {

    @Test
    void readsEachActiveSourceOnceThenReturnsNull() {
        Source hackerNews = activeSource(1L, "Hacker News");
        Source techBlog = activeSource(2L, "Tech Blog");
        ActiveSourceItemReader reader = new ActiveSourceItemReader(new FakeLoadActiveSourcesPort(hackerNews, techBlog));

        assertThat(reader.read()).isSameAs(hackerNews);
        assertThat(reader.read()).isSameAs(techBlog);
        assertThat(reader.read()).isNull();
    }

    @Test
    void returnsNullImmediatelyWhenNoActiveSources() {
        ActiveSourceItemReader reader = new ActiveSourceItemReader(new FakeLoadActiveSourcesPort());

        assertThat(reader.read()).isNull();
    }

    @Test
    void loadsActiveSourcesOnlyOnceAcrossReads() {
        FakeLoadActiveSourcesPort port = new FakeLoadActiveSourcesPort(activeSource(1L, "Hacker News"));
        ActiveSourceItemReader reader = new ActiveSourceItemReader(port);

        reader.read();
        reader.read();

        assertThat(port.loadActiveCallCount()).isEqualTo(1);
    }

    private static Source activeSource(Long id, String name) {
        return Source.restore(id, name, SourceType.RSS, "https://example.com/rss", "en", Category.DEV, true, null);
    }

    private static class FakeLoadActiveSourcesPort implements LoadActiveSourcesPort {
        private final List<Source> sources;
        private int loadActiveCallCount = 0;

        FakeLoadActiveSourcesPort(Source... sources) {
            this.sources = new ArrayList<>(List.of(sources));
        }

        int loadActiveCallCount() {
            return loadActiveCallCount;
        }

        @Override
        public List<Source> loadActive() {
            loadActiveCallCount++;
            return sources;
        }

        @Override
        public Optional<Source> findActiveById(Long sourceId) {
            return sources.stream().filter(s -> s.getSourceId().equals(sourceId)).findFirst();
        }
    }
}
