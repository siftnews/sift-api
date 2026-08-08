package com.siftnews.subscriber.application.port.out;

import com.siftnews.subscriber.domain.Subscriber;

import java.util.List;

public interface SaveSubscriberPort {

    Subscriber save(Subscriber subscriber);

    /**
     * 신규 구독자 시드 데이터를 bulk 저장하고 실제 신규 건수를 돌려준다. 구현체는 이메일
     * UNIQUE 제약과 충돌해도 예외 없이 재실행할 수 있어야 한다.
     */
    int saveNew(List<Subscriber> subscribers);
}
