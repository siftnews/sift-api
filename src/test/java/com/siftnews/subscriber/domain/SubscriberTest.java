package com.siftnews.subscriber.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriberTest {
    @Test
    void normalizesEmailAndCreatesActiveSubscriber() {
        Subscriber subscriber = Subscriber.create(" User@Example.COM ", 9);

        assertThat(subscriber.getEmail()).isEqualTo("user@example.com");
        assertThat(subscriber.getStatus()).isEqualTo(SubscriberStatus.ACTIVE);
        assertThat(subscriber.getPreferredSendHour()).isEqualTo(9);
    }

    @Test
    void rejectsInvalidSendHour() {
        assertThatThrownBy(() -> Subscriber.create("user@example.com", 24))
                .isInstanceOf(SubscriberException.class);
    }

    @Test
    void rejectsInvalidEmail() {
        assertThatThrownBy(() -> Subscriber.create("not-an-email", 9))
                .isInstanceOf(SubscriberException.class);
    }
}
