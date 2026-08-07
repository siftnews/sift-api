package com.siftnews.subscriber.adapter.out.persistence;

import com.siftnews.subscriber.application.port.out.LoadSubscriptionPort;
import com.siftnews.subscriber.application.port.out.LoadTopicSubscribersPort;
import com.siftnews.subscriber.api.DeliveryRecipient;
import com.siftnews.subscriber.application.port.out.SaveSubscriptionPort;
import com.siftnews.subscriber.domain.ConflictException;
import com.siftnews.subscriber.domain.Subscription;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class SubscriptionPersistenceAdapter implements LoadSubscriptionPort, SaveSubscriptionPort, LoadTopicSubscribersPort {

    private final SubscriptionJpaRepository repository;

    @Override
    public Optional<Subscription> load(Long subscriberId, Long topicId) {
        return repository.findBySubscriberIdAndTopicId(subscriberId, topicId)
                .map(SubscriptionMapper::toDomain);
    }

    @Override
    public java.util.List<DeliveryRecipient> loadActive(Long topicId, int preferredSendHour) {
        return repository.findActiveRecipients(topicId, preferredSendHour);
    }

    @Override
    @Transactional
    public Subscription save(Subscription subscription) {
        if (subscription.getSubscriptionId() == null) {
            try {
                return SubscriptionMapper.toDomain(
                        repository.saveAndFlush(SubscriptionMapper.toEntity(subscription)));
            } catch (DataIntegrityViolationException exception) {
                if (containsConstraint(exception, "uk_subscription_subscriber_topic")) {
                    throw new ConflictException("이미 존재하는 구독입니다.", exception);
                }
                throw exception;
            }
        }

        SubscriptionJpaEntity entity = repository.findById(subscription.getSubscriptionId()).orElseThrow();
        entity.updateStatus(subscription.getStatus());
        return SubscriptionMapper.toDomain(entity);
    }

    private boolean containsConstraint(Throwable throwable, String constraintName) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof org.hibernate.exception.ConstraintViolationException violation
                    && constraintName.equals(violation.getConstraintName())) {
                return true;
            }
            if (current.getMessage() != null && current.getMessage().contains(constraintName)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
