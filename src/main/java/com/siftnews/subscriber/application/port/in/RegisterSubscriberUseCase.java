package com.siftnews.subscriber.application.port.in;

public interface RegisterSubscriberUseCase {

    SubscriberSummary register(String email, int preferredSendHour);
}
