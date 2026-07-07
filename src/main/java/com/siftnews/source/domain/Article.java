package com.siftnews.source.domain;

import lombok.Getter;

import java.net.URI;
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
        String normalizedUrl = normalize(raw.url());
        return new Article(null, sourceId, raw.url(), normalizedUrl,
                raw.title(), raw.body(), raw.lang(), raw.publishedAt(), raw.category());
    }

    private static String normalize(String url) {
        if (url == null || url.isBlank()) {
            throw new ArticleException("article url은 비어 있을 수 없습니다.");
        }
        URI uri = URI.create(url);
        String host = uri.getHost().toLowerCase();
        return uri.getScheme() + "://" + host + uri.getPath();
    }
}
