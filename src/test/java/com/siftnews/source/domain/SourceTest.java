package com.siftnews.source.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void createLeavesIdAndLastCrawledAtEmpty() {
        Source source = Source.create("Hacker News", SourceType.RSS,
                "https://news.ycombinator.com/rss", "en", Category.PROGRAMMING, true);

        assertThat(source.getSourceId()).isNull();
        assertThat(source.getLastCrawledAt()).isNull();
        assertThat(source.getName()).isEqualTo("Hacker News");
        assertThat(source.getCategory()).isEqualTo(Category.PROGRAMMING);
        assertThat(source.isActive()).isTrue();
    }

    @Test
    void createTrimsNameAndLowercasesLang() {
        Source source = Source.create("  토스 기술블로그  ", SourceType.RSS,
                "  https://toss.tech/rss.xml  ", "KO", Category.DEV, true);

        assertThat(source.getName()).isEqualTo("토스 기술블로그");
        assertThat(source.getLang()).isEqualTo("ko");
        assertThat(source.getUrl()).isEqualTo("https://toss.tech/rss.xml");
    }

    /**
     * 피드 url은 쿼리로 피드를 구분하는 경우가 있어 정규화하지 않는다 — 쿼리를 버리는
     * {@code UriNormalizer}(기사 중복 판정용)를 재사용하면 다른 피드를 가리키게 된다.
     */
    @Test
    void createPreservesQueryStringInUrl() {
        Source source = Source.create("전자신문 IT", SourceType.RSS,
                "https://www.etnews.com/rss/section.xml?id=02", "ko", Category.DEV, true);

        assertThat(source.getUrl()).isEqualTo("https://www.etnews.com/rss/section.xml?id=02");
    }

    @Test
    void createRejectsBlankName() {
        assertThatThrownBy(() -> Source.create("  ", SourceType.RSS,
                "https://toss.tech/rss.xml", "ko", Category.DEV, true))
                .isInstanceOf(SourceException.class)
                .hasMessageContaining("name");
    }

    @Test
    void createRejectsUrlWithoutHost() {
        assertThatThrownBy(() -> Source.create("깨진 소스", SourceType.RSS,
                "not-a-url", "ko", Category.DEV, true))
                .isInstanceOf(SourceException.class)
                .hasMessageContaining("http(s)");
    }

    @Test
    void createRejectsNonHttpScheme() {
        assertThatThrownBy(() -> Source.create("FTP 소스", SourceType.RSS,
                "ftp://example.com/feed.xml", "ko", Category.DEV, true))
                .isInstanceOf(SourceException.class)
                .hasMessageContaining("스킴");
    }

    @Test
    void createRejectsBlankLang() {
        assertThatThrownBy(() -> Source.create("소스", SourceType.RSS,
                "https://toss.tech/rss.xml", "  ", Category.DEV, true))
                .isInstanceOf(SourceException.class)
                .hasMessageContaining("lang");
    }

    @Test
    void createRejectsNullCategory() {
        assertThatThrownBy(() -> Source.create("소스", SourceType.RSS,
                "https://toss.tech/rss.xml", "ko", null, true))
                .isInstanceOf(SourceException.class)
                .hasMessageContaining("category");
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
