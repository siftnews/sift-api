package com.siftnews.content.domain;

import com.siftnews.common.BusinessException;

import java.time.Instant;

/**
 * 선별 파이프라인이 다루는 후보 기사 뷰 — Source가 소유하는 Article(D-018)을
 * content 모듈에서 참조할 수 있도록 <b>느슨하게 복사한 값 타입</b>이다.
 * <p>
 * content가 {@code source.domain.Article}을 직접 import 하면 Modulith 경계를 어기므로,
 * Source named interface가 이 타입으로 후보를 넘겨준다(실 배선은 selectionJob 배치에서, D-030).
 * <p>
 * {@code category}가 {@code Category} enum이 아니라 문자열인 것도 같은 이유다 — enum은
 * source 모듈 소유라 content가 import 할 수 없다. 토픽의 {@code sourceCategories}와
 * 대조되므로 대소문자 무시로 비교한다({@link TopicFilter}).
 * <p>
 * {@code dedupClusterId}는 직전 normalizeDedup 실행이 남긴 값이다(D-031). 화제성
 * 점수(trendScore)의 입력인 <b>클러스터 크기를 이 값으로 윈도우 안에서 세기 때문에</b>,
 * 같은 윈도우로 로드한 후보끼리만 집계해야 D-032 불변식 (2)가 지켜진다. 컷 탈락 기사나
 * 아직 클러스터링되지 않은 기사는 {@code null}이다.
 */
public record CandidateArticle(
        Long articleId,
        Long sourceId,
        String normalizedUrl,
        String title,
        String lang,
        String body,
        Instant publishedAt,
        String category,
        String dedupClusterId) {

    public CandidateArticle {
        if (articleId == null) {
            throw new BusinessException("후보 기사 articleId는 null일 수 없습니다.");
        }
        if (sourceId == null) {
            throw new BusinessException("후보 기사 sourceId는 null일 수 없습니다.");
        }
    }
}
