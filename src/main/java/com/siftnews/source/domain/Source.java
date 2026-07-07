package com.siftnews.source.domain;

import lombok.Getter;

import java.time.Instant;

@Getter
public class Source {

    private final Long sourceId;
    private final String name;
    private final SourceType type;
    private final String url;
    private final String lang;
    private final Category category;
    private final boolean active;
    private Instant lastCrawledAt;

    private Source(Long sourceId, String name, SourceType type, String url, String lang, Category category, boolean active, Instant lastCrawledAt) {
        this.sourceId = sourceId;
        this.name = name;
        this.type = type;
        this.url = url;
        this.lang = lang;
        this.category = category;
        this.active = active;
        this.lastCrawledAt = lastCrawledAt;
    }

    public static Source restore(Long sourceId, String name, SourceType type, String url, String lang, Category category, boolean active, Instant lastCrawledAt) {
        return new Source(sourceId, name, type, url, lang, category, active, lastCrawledAt);
    }

    public void markCrawled(Instant at) {
        if (at == null) {
            throw new SourceException("lastCrawledAt은 null일 수 없습니다.");
        }

        this.lastCrawledAt = at;
    }
}
