package com.siftnews.delivery.adapter.out.persistence;

import com.siftnews.delivery.domain.DeliveryTaskStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface DeliveryTaskJpaRepository extends JpaRepository<DeliveryTaskJpaEntity, Long> {

    List<DeliveryTaskJpaEntity> findByDeliveryJobIdAndStatusOrderByIdAsc(Long deliveryJobId,
                                                                          DeliveryTaskStatus status,
                                                                          Pageable pageable);

    List<DeliveryTaskJpaEntity> findByDeliveryJobIdAndStatusAndIdGreaterThanOrderByIdAsc(
            Long deliveryJobId, DeliveryTaskStatus status, Long afterTaskId, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("""
            update DeliveryTaskJpaEntity task
            set task.status = com.siftnews.delivery.domain.DeliveryTaskStatus.SENDING,
                task.lastError = null
            where task.id = :taskId
              and task.status = com.siftnews.delivery.domain.DeliveryTaskStatus.PENDING
            """)
    int claimPending(@Param("taskId") Long taskId);

    @Modifying(clearAutomatically = true)
    @Query("""
            update DeliveryTaskJpaEntity task
            set task.status = com.siftnews.delivery.domain.DeliveryTaskStatus.SENT,
                task.lastError = null,
                task.sentAt = CURRENT_TIMESTAMP
            where task.id = :taskId
              and task.status = com.siftnews.delivery.domain.DeliveryTaskStatus.SENDING
            """)
    int markSent(@Param("taskId") Long taskId);

    @Modifying(clearAutomatically = true)
    @Query("""
            update DeliveryTaskJpaEntity task
            set task.status = com.siftnews.delivery.domain.DeliveryTaskStatus.FAILED,
                task.lastError = :error
            where task.id = :taskId
              and task.status = com.siftnews.delivery.domain.DeliveryTaskStatus.SENDING
            """)
    int markFailed(@Param("taskId") Long taskId, @Param("error") String error);

    @Modifying
    @Query(value = """
            INSERT INTO delivery_task (created_at, updated_at, delivery_job_id, subscriber_id, email, status,
                                       attempt_count, idempotency_key)
            VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :deliveryJobId, :subscriberId, :email, :status,
                    0, :idempotencyKey)
            ON CONFLICT (idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("deliveryJobId") Long deliveryJobId,
                       @Param("subscriberId") Long subscriberId,
                       @Param("email") String email,
                       @Param("status") String status,
                       @Param("idempotencyKey") String idempotencyKey);
}
