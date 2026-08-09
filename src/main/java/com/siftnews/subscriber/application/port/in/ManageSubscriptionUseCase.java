package com.siftnews.subscriber.application.port.in;

public interface ManageSubscriptionUseCase {

    SubscriptionSummary subscribe(Long subscriberId, Long topicId);

    void unsubscribe(Long subscriberId, Long topicId);
}
