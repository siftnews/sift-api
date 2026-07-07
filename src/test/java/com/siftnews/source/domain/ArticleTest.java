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
