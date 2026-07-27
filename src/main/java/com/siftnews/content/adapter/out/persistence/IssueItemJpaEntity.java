package com.siftnews.content.adapter.out.persistence;

import com.siftnews.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** issue_item 테이블 매핑 — 한 호에 실린 기사와 게재 순서 (MVP-DESIGN §2). */
@Entity
@Table(name = "issue_item",
        uniqueConstraints = @UniqueConstraint(name = "uk_issue_item_issue_article",
                columnNames = {"issue_id", "article_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class IssueItemJpaEntity extends BaseEntity {

    @Column(nullable = false)
    private Long issueId;

    @Column(nullable = false)
    private Long articleId;

    @Column(name = "item_rank", nullable = false)
    private int rank;

    @Column(nullable = false)
    private double score;

    IssueItemJpaEntity(Long issueId, Long articleId, int rank, double score) {
        this.issueId = issueId;
        this.articleId = articleId;
        this.rank = rank;
        this.score = score;
    }
}
