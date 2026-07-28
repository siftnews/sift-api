package com.siftnews.content.domain;

import com.siftnews.common.BusinessException;

import java.time.Instant;

/**
 * 토픽 × 기사 관련도 점수 — {@code article_score} 테이블의 도메인 표현 (MVP-DESIGN §2).
 * <p>
 * {@code (articleId, topicId)}가 사실상의 식별자다 — 같은 윈도우를 다시 돌려도 행이
 * 늘어나지 않도록 영속 계층이 이 쌍에 UNIQUE를 걸고 upsert 한다(재실행 멱등, D-031의 교훈).
 */
public record ArticleScore(Long articleId, Long topicId, double score, ScoreBreakdown breakdown, Instant computedAt) {

    public ArticleScore {
        if (articleId == null) {
            throw new BusinessException("점수의 articleId는 null일 수 없습니다.");
        }
        if (topicId == null) {
            throw new BusinessException("점수의 topicId는 null일 수 없습니다.");
        }
        if (breakdown == null) {
            throw new BusinessException("점수의 breakdown은 null일 수 없습니다.");
        }
    }
}
