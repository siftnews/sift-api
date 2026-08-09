package com.siftnews.subscriber.adapter.out.persistence;

import com.siftnews.subscriber.application.port.out.LoadSubscriberPort;
import com.siftnews.subscriber.application.port.out.SaveSubscriberPort;
import com.siftnews.subscriber.domain.ConflictException;
import com.siftnews.subscriber.domain.Subscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class SubscriberPersistenceAdapter implements LoadSubscriberPort, SaveSubscriberPort {

    private static final int SEED_BATCH_SIZE = 1_000;

    private static final String INSERT_NEW_SUBSCRIBER = """
            INSERT INTO subscriber (email, status, preferred_send_hour, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (email) DO NOTHING
            """;

    private final SubscriberJpaRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    @Override
    public Optional<Subscriber> load(Long subscriberId) {
        return repository.findById(subscriberId).map(SubscriberMapper::toDomain);
    }

    @Override
    public Optional<Subscriber> loadByEmail(String email) {
        return repository.findByEmail(email).map(SubscriberMapper::toDomain);
    }

    @Override
    @Transactional
    public Subscriber save(Subscriber subscriber) {
        try {
            return SubscriberMapper.toDomain(
                    repository.saveAndFlush(SubscriberMapper.toEntity(subscriber)));
        } catch (DataIntegrityViolationException exception) {
            if (containsConstraint(exception, "uk_subscriber_email")) {
                throw new ConflictException("이미 등록된 email입니다.", exception);
            }
            throw exception;
        }
    }

    @Override
    @Transactional
    public int saveNew(List<Subscriber> subscribers) {
        if (subscribers.isEmpty()) {
            return 0;
        }

        Timestamp now = Timestamp.from(clock.instant());
        int[][] results = jdbcTemplate.batchUpdate(INSERT_NEW_SUBSCRIBER, subscribers,
                SEED_BATCH_SIZE, (statement, subscriber) -> {
                    statement.setString(1, subscriber.getEmail());
                    statement.setString(2, subscriber.getStatus().name());
                    statement.setInt(3, subscriber.getPreferredSendHour());
                    statement.setTimestamp(4, now);
                    statement.setTimestamp(5, now);
                });

        return Arrays.stream(results)
                .flatMapToInt(Arrays::stream)
                .filter(updated -> updated > 0)
                .sum();
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
