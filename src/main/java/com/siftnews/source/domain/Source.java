package com.siftnews.source.domain;

import lombok.Getter;

import java.net.URI;
import java.time.Instant;
import java.util.Locale;

/**
 * 수집 대상 애그리거트 — 기사를 끌어올 외부 피드 하나.
 * <p>
 * JPA·Spring 무의존 순수 도메인이다(D-009).
 */
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

    /**
     * 새 소스 생성 — 불변식을 검증한다. 아직 수집된 적이 없으므로 {@code lastCrawledAt}은 null이다.
     */
    public static Source create(String name, SourceType type, String url, String lang, Category category, boolean active) {
        if (name == null || name.isBlank()) {
            throw new SourceException("소스 name은 비어 있을 수 없습니다.");
        }
        if (type == null) {
            throw new SourceException("소스 type은 null일 수 없습니다.");
        }
        if (category == null) {
            throw new SourceException("소스 category는 null일 수 없습니다.");
        }
        return new Source(null, name.strip(), type, validateFeedUrl(url), normalizeLang(lang), category, active, null);
    }

    /**
     * 영속 계층에서 복원 — 이미 저장된 값이므로 검증 없이 그대로 매핑한다.
     */
    public static Source restore(Long sourceId, String name, SourceType type, String url, String lang, Category category, boolean active, Instant lastCrawledAt) {
        return new Source(sourceId, name, type, url, lang, category, active, lastCrawledAt);
    }

    /**
     * 피드 url은 형식만 검증하고 <b>값은 원본 그대로</b> 둔다.
     * <p>
     * {@link com.siftnews.common.UriNormalizer}를 쓰지 않는 이유: 그쪽은 기사 중복 판정용이라
     * 쿼리스트링을 버리는데, 피드 url은 쿼리로 피드를 구분하는 경우가 있어(예: {@code ?feed=rss2},
     * {@code ?id=02}) 정규화하면 다른 피드를 가리키게 된다.
     */
    private static String validateFeedUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new SourceException("소스 url은 비어 있을 수 없습니다.");
        }

        String trimmed = url.strip();
        URI uri;
        try {
            uri = URI.create(trimmed);
        } catch (IllegalArgumentException e) {
            throw new SourceException("유효하지 않은 소스 url 형식입니다: " + url, e);
        }

        if (uri.getScheme() == null || uri.getHost() == null) {
            throw new SourceException("유효한 http(s) url이 아닙니다: " + url);
        }
        if (!uri.getScheme().equalsIgnoreCase("http") && !uri.getScheme().equalsIgnoreCase("https")) {
            throw new SourceException("지원되지 않는 소스 url 스킴입니다: " + uri.getScheme());
        }

        return trimmed;
    }

    private static String normalizeLang(String lang) {
        if (lang == null || lang.isBlank()) {
            throw new SourceException("소스 lang은 비어 있을 수 없습니다.");
        }
        return lang.strip().toLowerCase(Locale.ROOT);
    }

    public void markCrawled(Instant at) {
        if (at == null) {
            throw new SourceException("lastCrawledAt은 null일 수 없습니다.");
        }

        this.lastCrawledAt = at;
    }
}
