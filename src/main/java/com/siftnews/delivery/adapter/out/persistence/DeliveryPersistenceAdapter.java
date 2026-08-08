package com.siftnews.delivery.adapter.out.persistence;

import com.siftnews.delivery.application.port.out.LoadDeliveryJobPort;
import com.siftnews.delivery.application.port.out.LoadPendingDeliveryTasksPort;
import com.siftnews.delivery.application.port.out.LoadRetriableDeliveryTasksPort;
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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class DeliveryPersistenceAdapter implements LoadDeliveryJobPort, SaveDeliveryJobPort, SaveDeliveryTaskPort,
        LoadPendingDeliveryTasksPort, LoadRetriableDeliveryTasksPort, UpdateDeliveryTaskPort {

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
                        entity.getAttemptCount(), entity.getNextRetryAt(), entity.getLastError(),
                        entity.getIdempotencyKey()))
                .toList();
    }

    @Override
    public List<DeliveryTask> loadRetriable(java.time.Instant now, Long afterTaskId, int maxAttempts, int limit) {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("최대 발송 시도 횟수는 0보다 커야 합니다: " + maxAttempts);
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("task 페이지 크기는 0보다 커야 합니다: " + limit);
        }
        List<DeliveryTaskJpaEntity> page = deliveryTaskJpaRepository.findRetriable(
                now, maxAttempts, afterTaskId, PageRequest.of(0, limit));
        Map<Long, Long> issueIdByJobId = deliveryJobJpaRepository.findAllById(page.stream()
                        .map(DeliveryTaskJpaEntity::getDeliveryJobId)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(DeliveryJobJpaEntity::getId, DeliveryJobJpaEntity::getIssueId,
                        (first, ignored) -> first));
        return page.stream()
                .map(entity -> toDomain(entity, issueIdByJobId.get(entity.getDeliveryJobId())))
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int claimPending(Long taskId) {
        return deliveryTaskJpaRepository.claimPending(taskId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int claimFailed(Long taskId, java.time.Instant now, int maxAttempts) {
        return deliveryTaskJpaRepository.claimFailed(taskId, now, maxAttempts);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int markSent(Long taskId) {
        return deliveryTaskJpaRepository.markSent(taskId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int markFailed(Long taskId, String error, int attemptCount, java.time.Instant nextRetryAt) {
        return deliveryTaskJpaRepository.markFailed(taskId, error, attemptCount, nextRetryAt);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int markDead(Long taskId, String error) {
        return deliveryTaskJpaRepository.markDead(taskId, error);
    }

    private DeliveryJob toDomain(DeliveryJobJpaEntity entity) {
        return DeliveryJob.restore(entity.getId(), entity.getIssueId(), entity.getTotalCount(), entity.getStatus());
    }

    private DeliveryTask toDomain(DeliveryTaskJpaEntity entity, Long issueId) {
        if (issueId == null) {
            throw new IllegalStateException("발송 작업의 이슈를 찾을 수 없습니다: jobId=" + entity.getDeliveryJobId());
        }
        return DeliveryTask.restore(entity.getId(), entity.getDeliveryJobId(), issueId, entity.getSubscriberId(),
                entity.getEmail(), entity.getStatus(), entity.getAttemptCount(), entity.getNextRetryAt(),
                entity.getLastError(), entity.getIdempotencyKey());
    }
}
