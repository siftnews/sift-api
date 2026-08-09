package com.siftnews.source.api;

import java.time.Instant;

/**
 * 경계를 건너가는 기사 뷰 — 선별이 필요로 하는 필드만 담는다.
 * <p>
 * {@code source.domain.Article}을 그대로 넘기지 않는 이유: 애그리거트를 공개하면 Content가
 * Source의 내부 구조 변경에 묶인다. 이 record는 <b>계약</b>이라 필드를 늘릴 때 의도적 결정이 된다.
 * {@code category}가 enum이 아니라 문자열인 것도 같은 이유다 — {@code Category}는 Source internal이다.
 */
public record ArticleCandidate(
        Long articleId,
        Long sourceId,
        String normalizedUrl,
        String title,
        String lang,
        String body,
        Instant publishedAt,
        String category,
        String dedupClusterId) {
}
