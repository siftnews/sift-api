package com.siftnews.subscriber.application.service;

import com.siftnews.subscriber.application.port.out.LoadSubscriberPort;
import com.siftnews.subscriber.application.port.out.SaveSubscriberPort;
import com.siftnews.subscriber.domain.Subscriber;
import com.siftnews.subscriber.domain.SubscriberException;
import com.siftnews.subscriber.domain.SubscriberStatus;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriberServiceTest {
    @Test
    void registersNewSubscriber() {
        LoadSubscriberPort load = new LoadSubscriberPort() {
            public Optional<Subscriber> load(Long id) { return Optional.empty(); }
            public Optional<Subscriber> loadByEmail(String email) { return Optional.empty(); }
        };
        SaveSubscriberPort save = subscriber -> Subscriber.restore(10L, subscriber.getEmail(), subscriber.getStatus(), subscriber.getPreferredSendHour());
        SubscriberService service = new SubscriberService(load, save);

        var result = service.register("user@example.com", 8);

        assertThat(result.subscriberId()).isEqualTo(10L);
        assertThat(result.email()).isEqualTo("user@example.com");
    }

    @Test
    void rejectsDuplicateEmail() {
        Subscriber existing = Subscriber.restore(10L, "user@example.com", SubscriberStatus.ACTIVE, 8);
        LoadSubscriberPort load = new LoadSubscriberPort() {
            public Optional<Subscriber> load(Long id) { return Optional.empty(); }
            public Optional<Subscriber> loadByEmail(String email) { return Optional.of(existing); }
        };
        SubscriberService service = new SubscriberService(load, subscriber -> subscriber);

        assertThatThrownBy(() -> service.register("USER@example.com", 8))
                .isInstanceOf(SubscriberException.class);
    }
}
