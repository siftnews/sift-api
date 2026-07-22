package com.siftnews.content.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface TopicJpaRepository extends JpaRepository<TopicJpaEntity, Long> {

    boolean existsBySlug(String slug);
}
