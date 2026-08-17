package com.siftnews.content.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

interface ArticleScoreJpaRepository extends JpaRepository<ArticleScoreJpaEntity, Long> {

    List<ArticleScoreJpaEntity> findByTopicId(Long topicId);

    List<ArticleScoreJpaEntity> findByTopicIdAndComputedAtGreaterThanEqual(Long topicId, Instant computedAtFrom);
}
