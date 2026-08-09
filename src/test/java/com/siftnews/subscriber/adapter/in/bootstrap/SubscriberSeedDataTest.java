package com.siftnews.subscriber.adapter.in.bootstrap;

import com.siftnews.subscriber.domain.Subscriber;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriberSeedDataTest {

    private static final String EMAIL_DOMAIN = "seed-test.sift.local";

    @Test
    void generatesConfiguredLoadtestSizeWithUniqueEmails() {
        var subscribers = SubscriberSeedData.subscribers(100_000, EMAIL_DOMAIN);

        assertThat(subscribers).hasSize(100_000);
        assertThat(subscribers).extracting(Subscriber::getEmail)
                .doesNotHaveDuplicates()
                .allMatch(email -> email.endsWith("@" + EMAIL_DOMAIN));
        assertThat(subscribers.get(0).getEmail()).isEqualTo("subscriber-000001@" + EMAIL_DOMAIN);
        assertThat(subscribers.get(99_999).getEmail()).isEqualTo("subscriber-100000@" + EMAIL_DOMAIN);
    }

    @Test
    void repeatsDeterministicMorningPeakDistribution() {
        var subscribers = SubscriberSeedData.subscribers(48, EMAIL_DOMAIN);
        var firstPattern = Arrays.stream(SubscriberSeedData.preferredSendHours()).boxed().toList();

        assertThat(subscribers.subList(0, firstPattern.size()))
                .extracting(Subscriber::getPreferredSendHour)
                .containsExactlyElementsOf(firstPattern);
        assertThat(subscribers.subList(firstPattern.size(), firstPattern.size() * 2))
                .extracting(Subscriber::getPreferredSendHour)
                .containsExactlyElementsOf(firstPattern);
    }

    @Test
    void rejectsNegativeCountAndBlankDomain() {
        assertThatThrownBy(() -> SubscriberSeedData.subscribers(-1, EMAIL_DOMAIN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SubscriberSeedData.subscribers(1, " "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
