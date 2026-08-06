package com.siftnews.subscriber.adapter.out.persistence;

import com.siftnews.subscriber.application.port.out.LoadSubscriberPort;
import com.siftnews.subscriber.application.port.out.SaveSubscriberPort;
import com.siftnews.subscriber.domain.Subscriber;
import lombok.RequiredArgsConstructor;
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
        return SubscriberMapper.toDomain(repository.save(SubscriberMapper.toEntity(subscriber)));
    }
}
