package com.siftnews.content.domain;

import com.siftnews.common.BusinessException;

import java.time.Instant;

/**
 * 토픽 × 기사 관련도 점수 — {@code article_score} 테이블의 도메인 표현 (MVP-DESIGN §2).
 * <p>
 * {@code sourceId}를 함께 들고 있는 것은 랭킹의 <b>소스 쏠림 완화</b>가 이 값을 필요로 하는데,
 * article은 Source 소유(D-018)라 content가 조인할 수 없기 때문이다 — 점수를 계산하는 시점에
 * 후보 뷰에서 이미 알고 있는 값이라 같이 저장해 두면 랭킹이 content 안에서 끝난다.
 * <p>
 * {@code (articleId, topicId)}가 사실상의 식별자다 — 같은 윈도우를 다시 돌려도 행이
 * 늘어나지 않도록 영속 계층이 이 쌍에 UNIQUE를 걸고 upsert 한다(재실행 멱등, D-031의 교훈).
 */
public record ArticleScore(Long articleId, Long sourceId, Long topicId, double score,
                           ScoreBreakdown breakdown, Instant computedAt) {

    public ArticleScore {
        if (articleId == null) {
            throw new BusinessException("점수의 articleId는 null일 수 없습니다.");
        }
        if (sourceId == null) {
            throw new BusinessException("점수의 sourceId는 null일 수 없습니다.");
        }
        if (topicId == null) {
            throw new BusinessException("점수의 topicId는 null일 수 없습니다.");
        }
        if (breakdown == null) {
            throw new BusinessException("점수의 breakdown은 null일 수 없습니다.");
        }
    }
}
