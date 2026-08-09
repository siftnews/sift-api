package com.siftnews.subscriber.api;

public record DeliveryRecipient(
        Long subscriberId,
        String email
) {
}
