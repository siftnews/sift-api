package com.siftnews.content.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface TopicJpaRepository extends JpaRepository<TopicJpaEntity, Long> {

    java.util.List<TopicJpaEntity> findByActiveTrue();

    boolean existsBySlug(String slug);
}
