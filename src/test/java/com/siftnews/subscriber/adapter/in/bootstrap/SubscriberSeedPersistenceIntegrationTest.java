package com.siftnews.subscriber.adapter.in.bootstrap;

import com.siftnews.subscriber.application.port.in.SeedSubscribersUseCase;
import com.siftnews.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriberSeedPersistenceIntegrationTest extends AbstractIntegrationTest {

    private static final String EMAIL_DOMAIN = "persistence-seed-test.sift.local";

    @Autowired
    private SeedSubscribersUseCase seedSubscribersUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void insertsSeedSubscribersIdempotently() {
        var subscribers = SubscriberSeedData.subscribers(24, EMAIL_DOMAIN);

        assertThat(seedSubscribersUseCase.seed(subscribers)).isEqualTo(24);
        assertThat(seedSubscribersUseCase.seed(subscribers)).isZero();
        assertThat(countSeedSubscribers()).isEqualTo(24);
    }

    private int countSeedSubscribers() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM subscriber WHERE email LIKE ?",
                Integer.class,
                "subscriber-%@" + EMAIL_DOMAIN);
    }
}
