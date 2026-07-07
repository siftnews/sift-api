package com.siftnews.source.domain;

import java.time.Instant;

public record RawArticle(
        String url,
        String title,
        String body,
        String lang,
        Instant publishedAt,
        Category category
) {
}
