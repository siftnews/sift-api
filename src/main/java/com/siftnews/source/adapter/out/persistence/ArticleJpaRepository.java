package com.siftnews.source.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

interface ArticleJpaRepository extends JpaRepository<ArticleJpaEntity, Long> {

    List<ArticleJpaEntity> findByNormalizedUrlIn(List<String> normalizedUrls);

    List<ArticleJpaEntity> findByIdIn(List<Long> ids);

    /** {@code [from, to)} — from 포함, to 미포함. 기준 컬럼은 {@code created_at}(D-032). */
    List<ArticleJpaEntity> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant from, Instant to);

}
