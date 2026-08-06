package com.siftnews.subscriber.adapter.out.persistence;

import com.siftnews.subscriber.application.port.out.LoadSubscriptionPort;
import com.siftnews.subscriber.application.port.out.SaveSubscriptionPort;
import com.siftnews.subscriber.domain.Subscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class SubscriptionPersistenceAdapter implements LoadSubscriptionPort, SaveSubscriptionPort {

    private final SubscriptionJpaRepository repository;

    @Override
    public Optional<Subscription> load(Long subscriberId, Long topicId) {
        return repository.findBySubscriberIdAndTopicId(subscriberId, topicId)
                .map(SubscriptionMapper::toDomain);
    }

    @Override
    @Transactional
    public Subscription save(Subscription subscription) {
        if (subscription.getSubscriptionId() == null) {
            return SubscriptionMapper.toDomain(repository.save(SubscriptionMapper.toEntity(subscription)));
        }

        SubscriptionJpaEntity entity = repository.findById(subscription.getSubscriptionId()).orElseThrow();
        entity.updateStatus(subscription.getStatus());
        return SubscriptionMapper.toDomain(entity);
    }
}
