package com.siftnews.source.api;

public record ArticleExcerpt(
        Long articleId,
        String title,
        String url
) {
}
