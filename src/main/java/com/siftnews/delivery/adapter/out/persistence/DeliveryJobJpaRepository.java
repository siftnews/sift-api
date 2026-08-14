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
            UPDATE delivery_job
               SET total_count = :totalCount,
                   updated_at = CURRENT_TIMESTAMP
             WHERE id = :deliveryJobId
            """, nativeQuery = true)
    int updateTotalCount(@Param("deliveryJobId") Long deliveryJobId,
                         @Param("totalCount") int totalCount);

    @Modifying
    @Query(value = """
            UPDATE delivery_job
               SET status = 'SENDING',
                   updated_at = CURRENT_TIMESTAMP
             WHERE id = :deliveryJobId
               AND status = 'CREATED'
            """, nativeQuery = true)
    int markSending(@Param("deliveryJobId") Long deliveryJobId);

    @Modifying
    @Query(value = """
            UPDATE delivery_job job
               SET status = 'DONE',
                   updated_at = CURRENT_TIMESTAMP
             WHERE job.status = 'SENDING'
               AND NOT EXISTS (
                   SELECT 1
                     FROM delivery_task task
                    WHERE task.delivery_job_id = job.id
                      AND task.status IN ('PENDING', 'SENDING', 'FAILED')
               )
            """, nativeQuery = true)
    int markCompletedJobs();

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
