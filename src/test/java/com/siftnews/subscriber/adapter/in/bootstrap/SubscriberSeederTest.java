package com.siftnews.subscriber.adapter.in.bootstrap;

import com.siftnews.subscriber.application.port.in.SeedSubscribersUseCase;
import com.siftnews.subscriber.domain.Subscriber;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriberSeederTest {

    @Test
    void passesConfiguredCountAndDomainToSeedUseCase() throws Exception {
        var fake = new FakeSeedSubscribersUseCase();

        new SubscriberSeeder(fake, 24, "runner-test.sift.local").run(null);

        assertThat(fake.subscribers).hasSize(24);
        assertThat(fake.subscribers)
                .extracting(Subscriber::getEmail)
                .allMatch(email -> email.endsWith("@runner-test.sift.local"));
    }

    private static final class FakeSeedSubscribersUseCase implements SeedSubscribersUseCase {

        private List<Subscriber> subscribers = List.of();

        @Override
        public int seed(List<Subscriber> subscribers) {
            this.subscribers = subscribers;
            return subscribers.size();
        }
    }
}
