package com.siftnews.subscriber.application.service;

import com.siftnews.subscriber.application.port.out.LoadSubscriberPort;
import com.siftnews.subscriber.application.port.out.SaveSubscriberPort;
import com.siftnews.subscriber.domain.ConflictException;
import com.siftnews.subscriber.domain.Subscriber;
import com.siftnews.subscriber.domain.SubscriberStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriberServiceTest {

    @Test
    void registersNewSubscriber() {
        SubscriberService service = new SubscriberService(
                new FakeLoadSubscriberPort(Optional.empty(), Optional.empty()),
                new FakeSaveSubscriberPort(subscriber -> Subscriber.restore(
                        10L, subscriber.getEmail(), subscriber.getStatus(), subscriber.getPreferredSendHour())));

        var result = service.register("user@example.com", 8);

        assertThat(result.subscriberId()).isEqualTo(10L);
        assertThat(result.email()).isEqualTo("user@example.com");
    }

    @Test
    void rejectsDuplicateEmail() {
        Subscriber existing = Subscriber.restore(
                10L, "user@example.com", SubscriberStatus.ACTIVE, 8);
        SubscriberService service = new SubscriberService(
                new FakeLoadSubscriberPort(Optional.empty(), Optional.of(existing)),
                new FakeSaveSubscriberPort(Function.identity()));

        assertThatThrownBy(() -> service.register("USER@example.com", 8))
                .isInstanceOf(ConflictException.class);
    }

    private record FakeLoadSubscriberPort(
            Optional<Subscriber> subscriber,
            Optional<Subscriber> subscriberByEmail
    ) implements LoadSubscriberPort {

        @Override
        public Optional<Subscriber> load(Long subscriberId) {
            return subscriber;
        }

        @Override
        public Optional<Subscriber> loadByEmail(String email) {
            return subscriberByEmail;
        }
    }

    private record FakeSaveSubscriberPort(Function<Subscriber, Subscriber> saver) implements SaveSubscriberPort {

        @Override
        public Subscriber save(Subscriber subscriber) {
            return saver.apply(subscriber);
        }

        @Override
        public int saveNew(List<Subscriber> subscribers) {
            return subscribers.size();
        }
    }
}
