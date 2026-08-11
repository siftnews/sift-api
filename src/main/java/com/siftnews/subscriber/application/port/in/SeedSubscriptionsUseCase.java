package com.siftnews.subscriber.application.port.in;

public interface SeedSubscriptionsUseCase {

    /**
     * loadtest 시드 구독을 topic에 연결하고 실제 신규 건수를 돌려준다.
     */
    int seed(Long topicId, String emailDomain);
}
