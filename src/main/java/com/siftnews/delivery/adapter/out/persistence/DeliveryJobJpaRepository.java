package com.siftnews.delivery.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface DeliveryJobJpaRepository extends JpaRepository<DeliveryJobJpaEntity, Long> {

    Optional<DeliveryJobJpaEntity> findByIssueId(Long issueId);

    @Modifying
    @Query(value = """
            INSERT INTO delivery_job (created_at, updated_at, issue_id, total_count, status)
            VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :issueId, :totalCount, :status)
            ON CONFLICT (issue_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("issueId") Long issueId,
                       @Param("totalCount") int totalCount,
                       @Param("status") String status);
}
