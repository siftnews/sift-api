package com.siftnews.subscriber.application.port.out;

import com.siftnews.subscriber.domain.Subscription;

public interface SaveSubscriptionPort {

    Subscription save(Subscription subscription);
}
