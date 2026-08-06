package com.siftnews.subscriber.domain;

import com.siftnews.common.BusinessException;

public class SubscriberException extends BusinessException {

    public SubscriberException(String message) {
        super(message);
    }

    public SubscriberException(String message, Throwable cause) {
        super(message, cause);
    }
}
