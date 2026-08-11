package com.siftnews.subscriber.adapter.out.persistence;

import com.siftnews.subscriber.application.port.out.LoadSubscriptionPort;
import com.siftnews.subscriber.application.port.out.LoadTopicSubscribersPort;
import com.siftnews.subscriber.api.DeliveryRecipient;
import com.siftnews.subscriber.application.port.out.SaveSubscriptionPort;
import com.siftnews.subscriber.application.port.out.SaveSubscriptionSeedPort;
import com.siftnews.subscriber.domain.ConflictException;
import com.siftnews.subscriber.domain.Subscription;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class SubscriptionPersistenceAdapter implements LoadSubscriptionPort, SaveSubscriptionPort,
        SaveSubscriptionSeedPort, LoadTopicSubscribersPort {

    private static final String INSERT_LOADTEST_SUBSCRIPTIONS = """
            INSERT INTO subscription (subscriber_id, topic_id, status, created_at, updated_at)
            SELECT subscriber.id, ?, 'ACTIVE', ?, ?
            FROM subscriber
            WHERE subscriber.status = 'ACTIVE'
              AND subscriber.email LIKE ?
            ON CONFLICT (subscriber_id, topic_id) DO NOTHING
            """;

    private final SubscriptionJpaRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    @Override
    public Optional<Subscription> load(Long subscriberId, Long topicId) {
        return repository.findBySubscriberIdAndTopicId(subscriberId, topicId)
                .map(SubscriptionMapper::toDomain);
    }

    @Override
    public List<DeliveryRecipient> loadActive(Long topicId, int preferredSendHour) {
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

    @Override
    @Transactional
    public int saveNew(Long topicId, String emailDomain) {
        if (topicId == null) {
            throw new IllegalArgumentException("loadtest subscription topicId가 필요합니다.");
        }
        if (emailDomain == null || emailDomain.isBlank()) {
            throw new IllegalArgumentException("loadtest subscription email domain이 필요합니다.");
        }

        Timestamp now = Timestamp.from(clock.instant());
        return jdbcTemplate.update(INSERT_LOADTEST_SUBSCRIPTIONS,
                topicId, now, now, "subscriber-%@" + emailDomain.strip());
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
