package com.siftnews.content.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * 선별 파이프라인이 다루는 후보 기사 뷰 — Source가 소유하는 Article(D-018)을
 * content 모듈에서 참조할 수 있도록 <b>느슨하게 복사한 값 타입</b>이다.
 * <p>
 * content가 {@code source.domain.Article}을 직접 import 하면 Modulith 경계를 어기므로,
 * Source named interface가 이 타입으로 후보를 넘겨준다(실 배선은 selectionJob 배치에서, D-030).
 */
public record CandidateArticle(
        Long articleId,
        String normalizedUrl,
        String title,
        String lang,
        String body,
        Instant publishedAt) {

    public CandidateArticle {
        Objects.requireNonNull(articleId, "articleId는 필수다");
    }
}
