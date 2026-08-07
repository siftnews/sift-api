package com.siftnews.delivery.adapter.out.persistence;

import com.siftnews.delivery.application.port.out.LoadDeliveryJobPort;
import com.siftnews.delivery.application.port.out.SaveDeliveryJobPort;
import com.siftnews.delivery.application.port.out.SaveDeliveryTaskPort;
import com.siftnews.delivery.domain.DeliveryJob;
import com.siftnews.delivery.domain.DeliveryTask;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
        try {
            return toDomain(deliveryJobJpaRepository.saveAndFlush(
                    new DeliveryJobJpaEntity(deliveryJob.getIssueId(), deliveryJob.getTotalCount(), deliveryJob.getStatus())));
        } catch (DataIntegrityViolationException exception) {
            return loadByIssueId(deliveryJob.getIssueId()).orElseThrow(() -> exception);
        }
    }

    @Override
    public int saveIfAbsent(List<DeliveryTask> deliveryTasks) {
        int saved = 0;
        for (DeliveryTask task : deliveryTasks) {
            try {
                deliveryTaskJpaRepository.saveAndFlush(new DeliveryTaskJpaEntity(task.getDeliveryJobId(),
                        task.getSubscriberId(), task.getEmail(), task.getStatus(), task.getIdempotencyKey()));
                saved++;
            } catch (DataIntegrityViolationException ignored) {
                // UNIQUE(issue_id, subscriber_id)에서 나온 멱등 키 충돌은 재실행의 정상 결과다.
            }
        }
        return saved;
    }

    private DeliveryJob toDomain(DeliveryJobJpaEntity entity) {
        return DeliveryJob.restore(entity.getId(), entity.getIssueId(), entity.getTotalCount(), entity.getStatus());
    }
}
