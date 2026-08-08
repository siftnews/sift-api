package com.siftnews.subscriber.application.service;

import com.siftnews.subscriber.application.port.in.SeedSubscribersUseCase;
import com.siftnews.subscriber.application.port.out.SaveSubscriberPort;
import com.siftnews.subscriber.domain.Subscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeedSubscribersService implements SeedSubscribersUseCase {

    private final SaveSubscriberPort saveSubscriberPort;

    @Override
    public int seed(List<Subscriber> subscribers) {
        int inserted = saveSubscriberPort.saveNew(subscribers);
        log.info("구독자 시드 완료: 요청 {}건 · 신규 {}건", subscribers.size(), inserted);
        return inserted;
    }
}
