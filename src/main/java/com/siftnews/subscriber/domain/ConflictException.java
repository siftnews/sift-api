package com.siftnews.subscriber.domain;

public class ConflictException extends SubscriberException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
