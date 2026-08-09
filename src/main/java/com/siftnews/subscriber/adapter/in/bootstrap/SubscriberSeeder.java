package com.siftnews.subscriber.adapter.in.bootstrap;

import com.siftnews.subscriber.application.port.in.SeedSubscribersUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 명시적인 {@code loadtest} 프로파일에서만 10만 구독자 시드를 실행하는 인바운드 어댑터.
 * <p>
 * 기본 local/test 기동과 운영 프로파일에서는 동작하지 않는다. 실행 예시는
 * {@code SPRING_PROFILES_ACTIVE=local,loadtest ./gradlew bootRun}이다.
 */
@Slf4j
@Component
@Profile("loadtest")
@Order(Ordered.HIGHEST_PRECEDENCE)
class SubscriberSeeder implements ApplicationRunner {

    private final SeedSubscribersUseCase seedSubscribersUseCase;
    private final int count;
    private final String emailDomain;

    SubscriberSeeder(
            SeedSubscribersUseCase seedSubscribersUseCase,
            @Value("${sift.load-test.subscribers.count:" + SubscriberSeedData.DEFAULT_COUNT + "}") int count,
            @Value("${sift.load-test.subscribers.email-domain:" + SubscriberSeedData.DEFAULT_EMAIL_DOMAIN + "}")
            String emailDomain
    ) {
        this.seedSubscribersUseCase = seedSubscribersUseCase;
        this.count = count;
        this.emailDomain = emailDomain;
    }

    @Override
    public void run(ApplicationArguments args) {
        int inserted = seedSubscribersUseCase.seed(SubscriberSeedData.subscribers(count, emailDomain));
        log.info("loadtest subscriber seeder 종료: 요청 {}건 · 신규 {}건", count, inserted);
    }
}
