package com.siftnews.subscriber.adapter.in.bootstrap;

import com.siftnews.content.api.TopicCatalog;
import com.siftnews.subscriber.application.port.in.SeedSubscriptionsUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * loadtest 구독 workload를 준비하는 인바운드 어댑터. subscriber 시드가 끝난 뒤 실행되며, topic
 * slug를 통해 현재 DB의 ID를 해석해 내부 ID를 설정에 노출하지 않는다.
 */
@Slf4j
@Component
@Profile("loadtest")
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
class SubscriptionSeeder implements ApplicationRunner {

    private final SeedSubscriptionsUseCase seedSubscriptionsUseCase;
    private final TopicCatalog topicCatalog;
    private final String emailDomain;
    private final String topicSlug;

    SubscriptionSeeder(
            SeedSubscriptionsUseCase seedSubscriptionsUseCase,
            TopicCatalog topicCatalog,
            @Value("${sift.load-test.subscribers.email-domain:" + SubscriberSeedData.DEFAULT_EMAIL_DOMAIN + "}")
            String emailDomain,
            @Value("${sift.load-test.workload.topic-slug:dev}") String topicSlug
    ) {
        this.seedSubscriptionsUseCase = seedSubscriptionsUseCase;
        this.topicCatalog = topicCatalog;
        this.emailDomain = emailDomain;
        this.topicSlug = topicSlug;
    }

    @Override
    public void run(ApplicationArguments args) {
        Long topicId = topicCatalog.findActiveBySlug(topicSlug)
                .map(reference -> reference.topicId())
                .orElseThrow(() -> new IllegalStateException(
                        "loadtest workload topic을 찾을 수 없습니다: slug=" + topicSlug));
        int inserted = seedSubscriptionsUseCase.seed(topicId, emailDomain);
        log.info("loadtest subscription seeder 종료: topicSlug={} · topicId={} · 신규 {}건",
                topicSlug, topicId, inserted);
    }
}
