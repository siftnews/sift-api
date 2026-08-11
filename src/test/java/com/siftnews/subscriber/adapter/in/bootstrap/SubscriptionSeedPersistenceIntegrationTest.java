package com.siftnews.subscriber.adapter.in.bootstrap;

import com.siftnews.subscriber.application.port.in.SeedSubscribersUseCase;
import com.siftnews.subscriber.application.port.out.SaveSubscriptionSeedPort;
import com.siftnews.support.AbstractIntegrationTest;
import com.siftnews.support.TestDatabaseFixtures;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionSeedPersistenceIntegrationTest extends AbstractIntegrationTest {

    private static final long TOPIC_ID = 990_001L;
    private static final String EMAIL_DOMAIN = "subscription-seed-test.sift.local";

    @Autowired
    private SeedSubscribersUseCase seedSubscribersUseCase;

    @Autowired
    private SaveSubscriptionSeedPort saveSubscriptionSeedPort;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void insertsLoadtestSubscriptionsIdempotently() {
        TestDatabaseFixtures.topic(entityManager, TOPIC_ID);
        seedSubscribersUseCase.seed(SubscriberSeedData.subscribers(24, EMAIL_DOMAIN));

        assertThat(saveSubscriptionSeedPort.saveNew(TOPIC_ID, EMAIL_DOMAIN)).isEqualTo(24);
        assertThat(saveSubscriptionSeedPort.saveNew(TOPIC_ID, EMAIL_DOMAIN)).isZero();
        assertThat(countSeedSubscriptions()).isEqualTo(24);
    }

    private int countSeedSubscriptions() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM subscription WHERE topic_id = ?", Integer.class, TOPIC_ID);
    }
}
