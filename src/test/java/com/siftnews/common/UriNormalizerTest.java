package com.siftnews.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UriNormalizerTest {

    @Test
    void normalizeRemovesQueryStringAndLowercasesHost() {
        String normalized = UriNormalizer.normalize("https://Example.COM/news/1?utm_source=abc&ref=xyz");

        assertThat(normalized).isEqualTo("https://example.com/news/1");
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
