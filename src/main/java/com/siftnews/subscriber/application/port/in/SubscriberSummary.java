package com.siftnews.subscriber.application.port.in;

import com.siftnews.subscriber.domain.Subscriber;

public record SubscriberSummary(
        Long subscriberId,
        String email,
        String status,
        int preferredSendHour
) {

    public static SubscriberSummary from(Subscriber subscriber) {
        return new SubscriberSummary(subscriber.getSubscriberId(), subscriber.getEmail(),
                subscriber.getStatus().name(), subscriber.getPreferredSendHour());
    }
}
