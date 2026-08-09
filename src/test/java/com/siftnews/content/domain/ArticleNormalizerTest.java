package com.siftnews.content.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleNormalizerTest {

    private static final String LONG_BODY = "x".repeat(250);

    private static CandidateArticle article(String lang, String body) {
        return new CandidateArticle(1L, 1L, "https://example.com/a", "제목", lang, body, Instant.now(), "DEV", null);
    }

    @Test
    void survivesWhenLangSupportedAndBodyLongEnough() {
        assertThat(ArticleNormalizer.survives(article("en", LONG_BODY))).isTrue();
        assertThat(ArticleNormalizer.survives(article("KO", LONG_BODY))).isTrue();
    }

    @Test
    void dropsUnsupportedLang() {
        assertThat(ArticleNormalizer.survives(article("fr", LONG_BODY))).isFalse();
        assertThat(ArticleNormalizer.survives(article(null, LONG_BODY))).isFalse();
    }

    @Test
    void dropsBodyBelowMinLength() {
        assertThat(ArticleNormalizer.survives(article("en", "너무 짧은 본문"))).isFalse();
        assertThat(ArticleNormalizer.survives(article("en", null))).isFalse();
    }
}
