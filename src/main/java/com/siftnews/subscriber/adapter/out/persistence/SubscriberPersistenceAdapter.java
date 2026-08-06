package com.siftnews.subscriber.adapter.out.persistence;

import com.siftnews.subscriber.application.port.out.LoadSubscriberPort;
import com.siftnews.subscriber.application.port.out.SaveSubscriberPort;
import com.siftnews.subscriber.domain.ConflictException;
import com.siftnews.subscriber.domain.Subscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class SubscriberPersistenceAdapter implements LoadSubscriberPort, SaveSubscriberPort {

    private final SubscriberJpaRepository repository;

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
