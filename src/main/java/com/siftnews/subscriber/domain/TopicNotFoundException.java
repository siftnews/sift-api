package com.siftnews.subscriber.domain;

public class TopicNotFoundException extends SubscriberException {

    public TopicNotFoundException(String message) {
        super(message);
    }
}
