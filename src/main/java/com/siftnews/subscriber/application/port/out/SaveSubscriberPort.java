package com.siftnews.subscriber.application.port.out;

import com.siftnews.subscriber.domain.Subscriber;

public interface SaveSubscriberPort {

    Subscriber save(Subscriber subscriber);
}
