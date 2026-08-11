package com.siftnews.subscriber.application.service;

import com.siftnews.subscriber.application.port.in.SeedSubscriptionsUseCase;
import com.siftnews.subscriber.application.port.out.SaveSubscriptionSeedPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeedSubscriptionsService implements SeedSubscriptionsUseCase {

    private final SaveSubscriptionSeedPort saveSubscriptionSeedPort;

    @Override
    public int seed(Long topicId, String emailDomain) {
        int inserted = saveSubscriptionSeedPort.saveNew(topicId, emailDomain);
        log.info("loadtest subscription 시드 완료: topicId={} · emailDomain={} · 신규 {}건",
                topicId, emailDomain, inserted);
        return inserted;
    }
}
