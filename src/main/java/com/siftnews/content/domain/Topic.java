package com.siftnews.content.domain;

import lombok.Getter;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 선별 기준 애그리거트 — 구독자가 고르는 관심 토픽(dev/ai/econ 등).
 * <p>
 * 후보 기사를 끌어올 소스 카테고리와, 필터·스코어링·랭킹에 쓰는 키워드/가중치/임계값을 담는다
 * (파이프라인 상세는 docs/SELECTION.md). JPA·Spring 무의존 순수 도메인이다(D-009).
 * <p>
 * {@code sourceCategories}는 source 모듈의 Category enum이 아니라 <b>문자열 태그</b>다 —
 * content 모듈이 source 내부에 코드 의존을 만들지 않도록 느슨하게 결합한다(Modulith 경계).
 */
@Getter
public class Topic {

    private final Long topicId;
    private final String name;
    private final String slug;
    private final String langScope;
    private final List<String> includeKeywords;
    private final List<String> excludeKeywords;
    private final Map<String, Double> keywordWeights;
    private final List<String> sourceCategories;
    private final int recencyHalfLifeHours;
    private final int maxItems;
    private final double scoreThreshold;
    private final boolean active;

    private Topic(Long topicId, String name, String slug, String langScope,
                  List<String> includeKeywords, List<String> excludeKeywords,
                  Map<String, Double> keywordWeights, List<String> sourceCategories,
                  int recencyHalfLifeHours, int maxItems, double scoreThreshold, boolean active) {
        this.topicId = topicId;
        this.name = name;
        this.slug = slug;
        this.langScope = langScope;
        this.includeKeywords = includeKeywords;
        this.excludeKeywords = excludeKeywords;
        this.keywordWeights = keywordWeights;
        this.sourceCategories = sourceCategories;
        this.recencyHalfLifeHours = recencyHalfLifeHours;
        this.maxItems = maxItems;
        this.scoreThreshold = scoreThreshold;
        this.active = active;
    }

    /**
     * 새 토픽 생성 — 불변식을 검증하고 컬렉션을 방어 복사한다(null은 빈 값으로).
     */
    public static Topic create(String name, String slug, String langScope,
                               List<String> includeKeywords, List<String> excludeKeywords,
                               Map<String, Double> keywordWeights, List<String> sourceCategories,
                               int recencyHalfLifeHours, int maxItems, double scoreThreshold, boolean active) {
        String normalizedSlug = normalizeSlug(slug);
        if (name == null || name.isBlank()) {
            throw new TopicException("토픽 name은 비어 있을 수 없습니다.");
        }
        if (recencyHalfLifeHours <= 0) {
            throw new TopicException("recencyHalfLifeHours는 0보다 커야 합니다: " + recencyHalfLifeHours);
        }
        if (maxItems <= 0) {
            throw new TopicException("maxItems는 0보다 커야 합니다: " + maxItems);
        }
        if (!Double.isFinite(scoreThreshold) || scoreThreshold < 0) {
            throw new TopicException("scoreThreshold는 0 이상의 유한값이어야 합니다: " + scoreThreshold);
        }
        return new Topic(null, name.strip(), normalizedSlug, langScope,
                copyOrEmpty(includeKeywords), copyOrEmpty(excludeKeywords),
                copyOrEmpty(keywordWeights), copyOrEmpty(sourceCategories),
                recencyHalfLifeHours, maxItems, scoreThreshold, active);
    }

    /**
     * 영속 계층에서 복원 — 이미 저장된 값이므로 검증 없이 그대로 매핑한다.
     */
    public static Topic restore(Long topicId, String name, String slug, String langScope,
                                List<String> includeKeywords, List<String> excludeKeywords,
                                Map<String, Double> keywordWeights, List<String> sourceCategories,
                                int recencyHalfLifeHours, int maxItems, double scoreThreshold, boolean active) {
        return new Topic(topicId, name, slug, langScope,
                copyOrEmpty(includeKeywords), copyOrEmpty(excludeKeywords),
                copyOrEmpty(keywordWeights), copyOrEmpty(sourceCategories),
                recencyHalfLifeHours, maxItems, scoreThreshold, active);
    }

    private static String normalizeSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new TopicException("토픽 slug는 비어 있을 수 없습니다.");
        }
        String normalized = slug.strip().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9-]+")) {
            throw new TopicException("토픽 slug는 소문자·숫자·하이픈만 허용합니다: " + slug);
        }
        return normalized;
    }

    private static List<String> copyOrEmpty(List<String> values) {
        if (values == null) {
            return List.of();
        }
        if (values.stream().anyMatch(Objects::isNull)) {
            throw new TopicException("컬렉션 요소는 null일 수 없습니다.");
        }
        return List.copyOf(values);
    }

    private static Map<String, Double> copyOrEmpty(Map<String, Double> values) {
        if (values == null) {
            return Map.of();
        }
        if (values.keySet().stream().anyMatch(Objects::isNull)
                || values.values().stream().anyMatch(Objects::isNull)) {
            throw new TopicException("맵의 키·값은 null일 수 없습니다.");
        }
        return Map.copyOf(values);
    }
}
