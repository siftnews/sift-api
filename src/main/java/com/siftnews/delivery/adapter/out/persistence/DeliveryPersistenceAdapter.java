package com.siftnews.delivery.adapter.out.persistence;

import com.siftnews.delivery.application.port.out.LoadDeliveryJobPort;
import com.siftnews.delivery.application.port.out.LoadPendingDeliveryTasksPort;
import com.siftnews.delivery.application.port.out.SaveDeliveryJobPort;
import com.siftnews.delivery.application.port.out.SaveDeliveryTaskPort;
import com.siftnews.delivery.application.port.out.UpdateDeliveryTaskPort;
import com.siftnews.delivery.domain.DeliveryJob;
import com.siftnews.delivery.domain.DeliveryTask;
import com.siftnews.delivery.domain.DeliveryTaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class DeliveryPersistenceAdapter implements LoadDeliveryJobPort, SaveDeliveryJobPort, SaveDeliveryTaskPort,
        LoadPendingDeliveryTasksPort, UpdateDeliveryTaskPort {

    private final DeliveryJobJpaRepository deliveryJobJpaRepository;
    private final DeliveryTaskJpaRepository deliveryTaskJpaRepository;

    @Override
    public Optional<DeliveryJob> loadByIssueId(Long issueId) {
        return deliveryJobJpaRepository.findByIssueId(issueId).map(this::toDomain);
    }

    @Override
    public DeliveryJob save(DeliveryJob deliveryJob) {
        deliveryJobJpaRepository.insertIfAbsent(deliveryJob.getIssueId(), deliveryJob.getTotalCount(),
                deliveryJob.getStatus().name());
        return loadByIssueId(deliveryJob.getIssueId())
                .orElseThrow(() -> new IllegalStateException("delivery job 저장 후 조회할 수 없습니다: issueId="
                        + deliveryJob.getIssueId()));
    }

    @Override
    public int saveIfAbsent(List<DeliveryTask> deliveryTasks) {
        int saved = 0;
        for (DeliveryTask task : deliveryTasks) {
            saved += deliveryTaskJpaRepository.insertIfAbsent(task.getDeliveryJobId(), task.getSubscriberId(),
                    task.getEmail(), task.getStatus().name(), task.getIdempotencyKey());
        }
        return saved;
    }

    @Override
    public List<DeliveryTask> loadPending(Long deliveryJobId, Long afterTaskId, int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("task 페이지 크기는 0보다 커야 합니다: " + limit);
        }
        Long issueId = deliveryJobJpaRepository.findById(deliveryJobId)
                .map(DeliveryJobJpaEntity::getIssueId)
                .orElseThrow(() -> new IllegalStateException("발송 작업을 찾을 수 없습니다: jobId=" + deliveryJobId));
        PageRequest pageRequest = PageRequest.of(0, limit);
        List<DeliveryTaskJpaEntity> page = afterTaskId == null
                ? deliveryTaskJpaRepository.findByDeliveryJobIdAndStatusOrderByIdAsc(deliveryJobId,
                DeliveryTaskStatus.PENDING, pageRequest)
                : deliveryTaskJpaRepository.findByDeliveryJobIdAndStatusAndIdGreaterThanOrderByIdAsc(
                deliveryJobId, DeliveryTaskStatus.PENDING, afterTaskId, pageRequest);
        return page
                .stream()
                .map(entity -> DeliveryTask.restore(entity.getId(), entity.getDeliveryJobId(),
                        issueId, entity.getSubscriberId(), entity.getEmail(), entity.getStatus(),
                        entity.getIdempotencyKey()))
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int claimPending(Long taskId) {
        return deliveryTaskJpaRepository.claimPending(taskId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int markSent(Long taskId) {
        return deliveryTaskJpaRepository.markSent(taskId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int markFailed(Long taskId, String error) {
        return deliveryTaskJpaRepository.markFailed(taskId, error);
    }

    private DeliveryJob toDomain(DeliveryJobJpaEntity entity) {
        return DeliveryJob.restore(entity.getId(), entity.getIssueId(), entity.getTotalCount(), entity.getStatus());
    }
}
