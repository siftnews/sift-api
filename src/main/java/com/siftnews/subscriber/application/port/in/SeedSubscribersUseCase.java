package com.siftnews.subscriber.application.port.in;

import com.siftnews.subscriber.domain.Subscriber;

import java.util.List;

public interface SeedSubscribersUseCase {

    /**
     * 구독자 시드 데이터를 저장하고 실제 신규 건수를 돌려준다.
     */
    int seed(List<Subscriber> subscribers);
}
