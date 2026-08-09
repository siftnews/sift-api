package com.siftnews.subscriber.application.port.out;

import com.siftnews.subscriber.domain.Subscription;

import java.util.Optional;

public interface LoadSubscriptionPort {

    Optional<Subscription> load(Long subscriberId, Long topicId);
}
