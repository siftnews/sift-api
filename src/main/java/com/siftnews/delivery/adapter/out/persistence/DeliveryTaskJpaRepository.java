package com.siftnews.delivery.adapter.out.persistence;

import com.siftnews.delivery.domain.DeliveryTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface DeliveryTaskJpaRepository extends JpaRepository<DeliveryTaskJpaEntity, Long> {

    List<DeliveryTaskJpaEntity> findByDeliveryJobIdAndStatusOrderByIdAsc(Long deliveryJobId,
                                                                          DeliveryTaskStatus status);

    @Modifying
    @Query("""
            update DeliveryTaskJpaEntity task
            set task.status = :status, task.lastError = :error
            where task.id = :taskId
            """)
    void updateStatus(@Param("taskId") Long taskId, @Param("status") DeliveryTaskStatus status,
                      @Param("error") String error);

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
