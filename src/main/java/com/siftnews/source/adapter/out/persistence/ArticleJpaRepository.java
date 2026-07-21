package com.siftnews.source.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ArticleJpaRepository extends JpaRepository<ArticleJpaEntity, Long> {

    List<ArticleJpaEntity> findByNormalizedUrlIn(List<String> normalizedUrls);
}
