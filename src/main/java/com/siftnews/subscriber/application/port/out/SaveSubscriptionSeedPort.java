package com.siftnews.subscriber.application.port.out;

public interface SaveSubscriptionSeedPort {

    /**
     * 결정적 loadtest subscriber email 패턴에 해당하는 신규 구독을 bulk 저장한다.
     */
    int saveNew(Long topicId, String emailDomain);
}
