package com.siftnews.content.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

interface ArticleScoreJpaRepository extends JpaRepository<ArticleScoreJpaEntity, Long> {

    List<ArticleScoreJpaEntity> findByTopicId(Long topicId);

    /**
     * {@code (article_id, topic_id)}가 이미 있으면 점수·근거·계산시각을 덮어쓴다.
     * <p>
     * "있는지 조회 → 없으면 insert"로 나누면 같은 토픽을 동시에 돌릴 때 유니크 제약에 걸려
     * 한쪽 실행이 죽는다. 확인과 쓰기를 한 문장으로 합쳐 재실행·동시 실행 모두 안전하게 만든다.
     * <p>
     * 네이티브 SQL인 이유는 {@code ON CONFLICT}가 JPQL에 없어서다({@code SourceJpaRepository}와
     * 같은 사정). {@code breakdown}은 문자열로 넘겨 {@code jsonb}로 캐스팅한다 — 읽기는
     * 엔티티의 {@code @JdbcTypeCode(SqlTypes.JSON)}가 그대로 되돌린다. 감사 컬럼은 JPA
     * Auditing을 우회하므로 호출자가 {@code now}를 넘긴다.
     */
    @Modifying
    @Query(value = """
            INSERT INTO article_score (article_id, topic_id, score, breakdown, computed_at, created_at, updated_at)
            VALUES (:articleId, :topicId, :score, CAST(:breakdown AS jsonb), :computedAt, :now, :now)
            ON CONFLICT (article_id, topic_id) DO UPDATE SET
                score = EXCLUDED.score,
                breakdown = EXCLUDED.breakdown,
                computed_at = EXCLUDED.computed_at,
                updated_at = EXCLUDED.updated_at
            """, nativeQuery = true)
    void upsert(@Param("articleId") Long articleId,
                @Param("topicId") Long topicId,
                @Param("score") double score,
                @Param("breakdown") String breakdown,
                @Param("computedAt") Instant computedAt,
                @Param("now") Instant now);
}
