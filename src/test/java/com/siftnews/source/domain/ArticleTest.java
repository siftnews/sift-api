package com.siftnews.source.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArticleTest {

    @Test
    void createNormalizesUrlViaUriNormalizer() {
        RawArticle raw = new RawArticle(
                "https://Example.COM/news/1?utm_source=abc&ref=xyz",
                "제목",
                "본문",
                "ko",
                Instant.parse("2026-07-01T00:00:00Z"),
                Category.DEV
        );

        Article article = Article.create(raw, 1L);

        assertThat(article.getNormalizedUrl()).isEqualTo("https://example.com/news/1");
    }

    /**
     * 쿼리로 기사를 구분하는 소스(AI타임스 {@code idxno})의 두 기사가 서로 다른 키를 갖는지 —
     * 같아지면 {@code UNIQUE(normalized_url)}에 걸려 뒤 기사가 통째로 유실된다.
     */
    @Test
    void createKeepsQueryThatIdentifiesArticle() {
        Article first = Article.create(rawWithUrl("https://www.aitimes.com/news/articleView.html?idxno=213427"), 1L);
        Article second = Article.create(rawWithUrl("https://www.aitimes.com/news/articleView.html?idxno=213385"), 1L);

        assertThat(first.getNormalizedUrl())
                .isEqualTo("https://www.aitimes.com/news/articleView.html?idxno=213427")
                .isNotEqualTo(second.getNormalizedUrl());
    }

    private static RawArticle rawWithUrl(String url) {
        return new RawArticle(url, "제목", "본문", "ko", Instant.parse("2026-07-01T00:00:00Z"), Category.AI);
    }

    @Test
    void createMapsRemainingFieldsDirectlyFromRawArticle() {
        Instant publishedAt = Instant.parse("2026-07-01T00:00:00Z");
        RawArticle raw = new RawArticle(
                "https://example.com/news/1",
                "제목",
                "본문",
                "ko",
                publishedAt,
                Category.DEV
        );

        Article article = Article.create(raw, 42L);

        assertThat(article.getSourceId()).isEqualTo(42L);
        assertThat(article.getUrl()).isEqualTo("https://example.com/news/1");
        assertThat(article.getTitle()).isEqualTo("제목");
        assertThat(article.getBody()).isEqualTo("본문");
        assertThat(article.getLang()).isEqualTo("ko");
        assertThat(article.getPublishedAt()).isEqualTo(publishedAt);
    }

    @Test
    void createMapsCategoryFromRawArticle() {
        RawArticle raw = new RawArticle(
                "https://example.com/news/1",
                "제목",
                "본문",
                "ko",
                Instant.parse("2026-07-01T00:00:00Z"),
                Category.AI
        );

        Article article = Article.create(raw, 1L);

        assertThat(article.getCategory()).isEqualTo(Category.AI);
    }

    @Test
    void createThrowsArticleExceptionWhenUrlIsBlank() {
        RawArticle raw = new RawArticle(
                "  ",
                "제목",
                "본문",
                "ko",
                Instant.parse("2026-07-01T00:00:00Z"),
                Category.DEV
        );

        assertThatThrownBy(() -> Article.create(raw, 1L))
                .isInstanceOf(ArticleException.class)
                .hasMessageContaining("url");
    }
}
