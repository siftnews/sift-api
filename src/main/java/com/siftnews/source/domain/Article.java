package com.siftnews.source.domain;

import com.siftnews.common.UriNormalizer;
import lombok.Getter;

import java.time.Instant;

@Getter
public class Article {

    private final Long articleId;
    private final Long sourceId;
    private final String url;
    private final String normalizedUrl;
    private final String title;
    private final String body;
    private final String lang;
    private final Instant publishedAt;
    private final Category category;

    private Article(Long articleId, Long sourceId, String url, String normalizedUrl, String title, String body, String lang, Instant publishedAt, Category category) {
        this.articleId = articleId;
        this.sourceId = sourceId;
        this.url = url;
        this.normalizedUrl = normalizedUrl;
        this.title = title;
        this.body = body;
        this.lang = lang;
        this.publishedAt = publishedAt;
        this.category = category;
    }

    public static Article create(RawArticle raw, Long sourceId) {
        String normalizedUrl = normalizeOrThrow(raw.url());
        return new Article(null, sourceId, raw.url(), normalizedUrl,
                raw.title(), raw.body(), raw.lang(), raw.publishedAt(), raw.category());
    }

    private static String normalizeOrThrow(String url) {
        try {
            return UriNormalizer.normalize(url);
        } catch (IllegalArgumentException e) {
            throw new ArticleException("article url이 유효하지 않습니다: " + url, e);
        }
    }
}
