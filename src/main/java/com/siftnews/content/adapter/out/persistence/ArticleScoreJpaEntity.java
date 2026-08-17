package com.siftnews.content.adapter.out.persistence;

import com.siftnews.common.BaseEntity;
import com.siftnews.content.domain.ScoreBreakdown;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * article_score 테이블 매핑 (MVP-DESIGN §2).
 * <p>
 * {@code UNIQUE(article_id, topic_id)}가 재실행 멱등의 근거다 — 같은 윈도우를 다시 돌려도
 * 행이 늘지 않고 갱신된다. 실제 덮어쓰기는 {@code ArticleScorePersistenceAdapter}의 batch upsert가 한다.
 * <p>
 * {@code breakdown}은 {@link ScoreBreakdown}을 그대로 JSON 컬럼에 싣는다 — topic의
 * {@code keyword_weights[json]}와 같은 관례({@code @JdbcTypeCode(SqlTypes.JSON)})다.
 * 도메인 값 타입을 엔티티가 참조하는 것은 {@code SourceJpaEntity}가 도메인 enum을
 * 그대로 쓰는 것과 같은 선이며(D-009는 애그리거트 POJO와 엔티티의 분리를 요구한다),
 * 근거 구조를 영속 전용으로 한 벌 더 두면 항목을 늘릴 때마다 두 곳을 고쳐야 한다.
 */
@Entity
@Table(name = "article_score",
        uniqueConstraints = @UniqueConstraint(name = "uk_article_score_article_topic",
                columnNames = {"article_id", "topic_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class ArticleScoreJpaEntity extends BaseEntity {

    @Column(nullable = false)
    private Long articleId;

    /** 랭킹의 소스 쏠림 완화가 쓰는 값 — article이 Source 소유라 조인 대신 비정규화한다(D-018). */
    @Column(nullable = false)
    private Long sourceId;

    @Column(nullable = false)
    private Long topicId;

    @Column(nullable = false)
    private double score;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private ScoreBreakdown breakdown;

    @Column(nullable = false)
    private Instant computedAt;

    ArticleScoreJpaEntity(Long articleId, Long sourceId, Long topicId, double score,
                          ScoreBreakdown breakdown, Instant computedAt) {
        this.articleId = articleId;
        this.sourceId = sourceId;
        this.topicId = topicId;
        this.score = score;
        this.breakdown = breakdown;
        this.computedAt = computedAt;
    }
}
