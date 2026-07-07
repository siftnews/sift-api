package com.siftnews.source.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SourceTest {

    @Test
    void isActiveReflectsActiveFlag() {
        Source source = Source.restore(1L, "Hacker News", SourceType.RSS,
                "https://news.ycombinator.com/rss", "en", Category.DEV, true, null);

        assertThat(source.isActive()).isTrue();
    }

    @Test
    void restoreMapsAllFields() {
        Source source = Source.restore(1L, "Hacker News", SourceType.RSS,
                "https://news.ycombinator.com/rss", "en", Category.DEV, true, null);

        assertThat(source.getSourceId()).isEqualTo(1L);
        assertThat(source.getName()).isEqualTo("Hacker News");
        assertThat(source.getType()).isEqualTo(SourceType.RSS);
        assertThat(source.getUrl()).isEqualTo("https://news.ycombinator.com/rss");
        assertThat(source.getLang()).isEqualTo("en");
        assertThat(source.getCategory()).isEqualTo(Category.DEV);
    }

    @Test
    void markCrawledUpdatesLastCrawledAt() {
        Source source = Source.restore(1L, "Hacker News", SourceType.RSS,
                "https://news.ycombinator.com/rss", "en", Category.DEV, true, null);
        Instant now = Instant.parse("2026-07-07T00:00:00Z");

        source.markCrawled(now);

        assertThat(source.getLastCrawledAt()).isEqualTo(now);
    }
}
