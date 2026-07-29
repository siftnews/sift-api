package com.siftnews.content.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface IssueItemJpaRepository extends JpaRepository<IssueItemJpaEntity, Long> {

    List<IssueItemJpaEntity> findByIssueIdOrderByRankAsc(Long issueId);

    void deleteByIssueId(Long issueId);
}
