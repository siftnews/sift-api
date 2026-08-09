package com.siftnews.subscriber.application.port.out;

import com.siftnews.subscriber.domain.Subscriber;

import java.util.Optional;

public interface LoadSubscriberPort {

    Optional<Subscriber> load(Long subscriberId);

    Optional<Subscriber> loadByEmail(String email);
}
