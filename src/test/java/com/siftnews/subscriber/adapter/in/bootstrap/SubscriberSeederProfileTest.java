package com.siftnews.subscriber.adapter.in.bootstrap;

import com.siftnews.subscriber.application.port.in.SeedSubscribersUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriberSeederProfileTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SubscriberSeederConfiguration.class)
            .withPropertyValues("spring.profiles.active=loadtest");

    @Test
    void createsSubscriberSeederForLoadtestProfile() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBeansOfType(SubscriberSeeder.class)).hasSize(1);
        });
    }

    @TestConfiguration(proxyBeanMethods = false)
    @Import(SubscriberSeeder.class)
    static class SubscriberSeederConfiguration {

        @Bean
        SeedSubscribersUseCase seedSubscribersUseCase() {
            return subscribers -> subscribers.size();
        }
    }
}
