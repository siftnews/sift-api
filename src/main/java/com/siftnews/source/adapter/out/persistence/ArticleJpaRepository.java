package com.siftnews.source.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

interface ArticleJpaRepository extends JpaRepository<ArticleJpaEntity, Long> {

    List<ArticleJpaEntity> findByNormalizedUrlIn(List<String> normalizedUrls);

    /** {@code [from, to)} — from 포함, to 미포함. 기준 컬럼은 {@code created_at}(D-032). */
    List<ArticleJpaEntity> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant from, Instant to);

    /**
     * 주어진 기사들의 클러스터 id를 한 값으로 바꾼다 — {@code clusterId}가 null이면 해제(D-031).
     * <p>
     * 값별로 묶어 호출하므로 쿼리 수는 <b>클러스터 수</b>에 비례한다. 건별 UPDATE(후보 수 비례)보다
     * 적고 MVP 윈도우 규모에서는 충분하다 — 병목으로 측정되면 CASE 식 한 방 UPDATE로 바꾼다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ArticleJpaEntity a set a.dedupClusterId = :clusterId where a.id in :ids")
    void updateDedupClusterId(@Param("clusterId") String clusterId, @Param("ids") Collection<Long> ids);
}
