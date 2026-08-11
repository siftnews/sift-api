package com.siftnews.delivery.adapter.out.email;

import com.siftnews.delivery.application.port.out.SendEmailPort;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class EmailAdapterProfileTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(EmailAdapterConfiguration.class)
            .withPropertyValues("spring.profiles.active=loadtest");

    @Test
    void selectsOnlyOneAdapterForLoadtestProfile() {
        contextRunner.run(context -> {
            assertThat(context.getBeansOfType(SendEmailPort.class))
                    .hasSize(1);
            assertThat(context.getBean(SendEmailPort.class))
                    .isInstanceOf(LoadtestSendEmailAdapter.class);
            assertThat(context.getBeansOfType(LocalSmtpAdapter.class)).isEmpty();
            assertThat(context.getBeansOfType(TestSendEmailAdapter.class)).isEmpty();
        });
    }

    @TestConfiguration(proxyBeanMethods = false)
    @Import({LoadtestSendEmailAdapter.class, LocalSmtpAdapter.class, TestSendEmailAdapter.class})
    static class EmailAdapterConfiguration {

        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}
