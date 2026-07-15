package com.siftnews.source.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface SourceJpaRepository extends JpaRepository<SourceJpaEntity, Long> {

    List<SourceJpaEntity> findByActiveTrue();

    Optional<SourceJpaEntity> findByIdAndActiveTrue(Long id);
}
