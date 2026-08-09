package com.siftnews.content.domain;

import com.siftnews.common.BusinessException;

/**
 * 이슈에 실린 기사 한 건 — 몇 번째로(rank), 몇 점으로(score) 뽑혔는지 (MVP-DESIGN §2).
 * <p>
 * {@code rank}는 1부터 시작하는 게재 순서다. 점수를 함께 남기는 이유는 나중에 "왜 이 순서였나"를
 * 되짚을 때 {@code article_score}를 다시 뒤지지 않아도 되게 하기 위함이다 — 점수는 재실행으로
 * 덮어써지지만 이 값은 그 호가 발행된 시점의 기록으로 남는다.
 */
public record IssueItem(Long articleId, int rank, double score) {

    public IssueItem {
        if (articleId == null) {
            throw new BusinessException("이슈 항목의 articleId는 null일 수 없습니다.");
        }
        if (rank < 1) {
            throw new BusinessException("이슈 항목의 rank는 1 이상이어야 합니다: " + rank);
        }
    }
}
