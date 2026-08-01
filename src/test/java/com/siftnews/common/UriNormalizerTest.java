package com.siftnews.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UriNormalizerTest {

    @Test
    void normalizeRemovesTrackingParamsAndLowercasesHost() {
        String normalized = UriNormalizer.normalize("https://Example.COM/news/1?utm_source=abc&ref=xyz");

        assertThat(normalized).isEqualTo("https://example.com/news/1");
    }

    @Test
    void normalizeKeepsNonTrackingQueryParams() {
        String normalized = UriNormalizer.normalize("https://example.com/news/articleView.html?idxno=213427&utm_source=abc");

        assertThat(normalized).isEqualTo("https://example.com/news/articleView.html?idxno=213427");
    }

    /**
     * 실측 회귀 (2026-08-01) — AI타임스는 {@code idxno}로 기사를 구분한다. 쿼리를 통째로 버리던
     * 시절에는 50건이 전부 같은 키가 돼 {@code UNIQUE(normalized_url)}에 걸려 1건만 적재됐다.
     */
    @Test
    void normalizeDistinguishesArticlesIdentifiedByQuery() {
        String first = UriNormalizer.normalize("https://www.aitimes.com/news/articleView.html?idxno=213427");
        String second = UriNormalizer.normalize("https://www.aitimes.com/news/articleView.html?idxno=213385");

        assertThat(first).isNotEqualTo(second);
    }

    /**
     * 실측 회귀 (2026-08-01) — BBC는 전 항목에 {@code at_medium}·{@code at_campaign}만 달아 보낸다.
     * 추적 파라미터를 걷어내면 쿼리가 비므로 {@code ?}를 남기지 않는다. 그래서 이 소스의 키는
     * 쿼리를 통째로 버리던 때와 값이 같고, 기존 적재분에 백필이 필요 없다.
     */
    @Test
    void normalizeOmitsQuestionMarkWhenAllParamsAreTracking() {
        String normalized = UriNormalizer.normalize(
                "https://www.bbc.co.uk/news/articles/c77g6dm5pr8o?at_medium=RSS&at_campaign=rss");

        assertThat(normalized).isEqualTo("https://www.bbc.co.uk/news/articles/c77g6dm5pr8o");
    }

    @Test
    void normalizeSortsQueryParamsSoOrderDoesNotMatter() {
        String ordered = UriNormalizer.normalize("https://example.com/news?a=1&b=2");
        String shuffled = UriNormalizer.normalize("https://example.com/news?b=2&a=1");

        assertThat(ordered).isEqualTo("https://example.com/news?a=1&b=2");
        assertThat(shuffled).isEqualTo(ordered);
    }

    @Test
    void normalizeTreatsTrackingParamNamesCaseInsensitively() {
        String normalized = UriNormalizer.normalize("https://example.com/news?UTM_Source=abc&FBCLID=xyz&id=7");

        assertThat(normalized).isEqualTo("https://example.com/news?id=7");
    }

    @Test
    void normalizeKeepsValuelessParam() {
        String normalized = UriNormalizer.normalize("https://example.com/news?print&id=7");

        assertThat(normalized).isEqualTo("https://example.com/news?id=7&print");
    }

    /**
     * 값에 들어 있던 {@code %26}(&amp;)이 구분자로 풀리면 파라미터 경계가 무너지므로
     * 인코딩된 원본을 그대로 다룬다.
     */
    @Test
    void normalizeKeepsPercentEncodingInQueryValue() {
        String normalized = UriNormalizer.normalize("https://example.com/news?q=a%26b");

        assertThat(normalized).isEqualTo("https://example.com/news?q=a%26b");
    }

    @Test
    void normalizeThrowsIllegalArgumentExceptionWhenUrlIsBlank() {
        assertThatThrownBy(() -> UriNormalizer.normalize("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("url");
    }

    @Test
    void normalizeThrowsIllegalArgumentExceptionWhenUrlIsMalformed() {
        assertThatThrownBy(() -> UriNormalizer.normalize("http://exa mple.com/path"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("url");
    }

    @Test
    void normalizeThrowsIllegalArgumentExceptionWhenUrlHasNoHost() {
        assertThatThrownBy(() -> UriNormalizer.normalize("mailto:test@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("url");
    }

    @Test
    void normalizeStripsDefaultHttpsPort() {
        String normalized = UriNormalizer.normalize("https://example.com:443/path");

        assertThat(normalized).isEqualTo("https://example.com/path");
    }

    @Test
    void normalizeKeepsNonDefaultPort() {
        String normalized = UriNormalizer.normalize("https://example.com:8443/path");

        assertThat(normalized).isEqualTo("https://example.com:8443/path");
    }

    @Test
    void normalizeConvertsEmptyPathToRootSlash() {
        String normalized = UriNormalizer.normalize("https://example.com");

        assertThat(normalized).isEqualTo("https://example.com/");
    }
}
