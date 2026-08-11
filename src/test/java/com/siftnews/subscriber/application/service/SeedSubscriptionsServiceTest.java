package com.siftnews.subscriber.application.service;

import com.siftnews.subscriber.application.port.out.SaveSubscriptionSeedPort;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SeedSubscriptionsServiceTest {

    @Test
    void returnsInsertedCountFromSeedPort() {
        var port = new RecordingSaveSubscriptionSeedPort(24);

        int inserted = new SeedSubscriptionsService(port).seed(42L, "loadtest.sift.local");

        assertThat(inserted).isEqualTo(24);
        assertThat(port.topicId).isEqualTo(42L);
        assertThat(port.emailDomain).isEqualTo("loadtest.sift.local");
    }

    private static final class RecordingSaveSubscriptionSeedPort implements SaveSubscriptionSeedPort {

        private final int inserted;
        private Long topicId;
        private String emailDomain;

        private RecordingSaveSubscriptionSeedPort(int inserted) {
            this.inserted = inserted;
        }

        @Override
        public int saveNew(Long topicId, String emailDomain) {
            this.topicId = topicId;
            this.emailDomain = emailDomain;
            return inserted;
        }
    }
}
