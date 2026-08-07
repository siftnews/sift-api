package com.siftnews.delivery.adapter.out.persistence;

import com.siftnews.delivery.application.port.out.LoadDeliveryJobPort;
import com.siftnews.delivery.application.port.out.SaveDeliveryJobPort;
import com.siftnews.delivery.application.port.out.SaveDeliveryTaskPort;
import com.siftnews.delivery.domain.DeliveryJob;
import com.siftnews.delivery.domain.DeliveryTask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class DeliveryPersistenceAdapter implements LoadDeliveryJobPort, SaveDeliveryJobPort, SaveDeliveryTaskPort {

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

    private DeliveryJob toDomain(DeliveryJobJpaEntity entity) {
        return DeliveryJob.restore(entity.getId(), entity.getIssueId(), entity.getTotalCount(), entity.getStatus());
    }
}
