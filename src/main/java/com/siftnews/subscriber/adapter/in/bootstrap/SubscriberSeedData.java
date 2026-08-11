package com.siftnews.subscriber.adapter.in.bootstrap;

import com.siftnews.subscriber.domain.Subscriber;

import java.util.List;
import java.util.stream.IntStream;

/**
 * M4 V1 부하 테스트용 구독자 시드 정의.
 * <p>
 * D-019의 현실적인 아침 피크 시나리오를 고정된 24시간 패턴으로 표현한다. 난수를 사용하지
 * 않으므로 같은 count와 email domain으로 실행하면 같은 email·수신 시각이 생성된다.
 */
final class SubscriberSeedData {

    static final int DEFAULT_COUNT = 100_000;
    static final String DEFAULT_EMAIL_DOMAIN = "loadtest.sift.local";

    private static final int[] PREFERRED_SEND_HOURS = {
            7, 7, 7, 7,
            8, 8, 8, 8, 8, 8,
            9, 9, 9, 9, 9,
            10, 10, 10,
            11, 11,
            12,
            13,
            14,
            18
    };

    private SubscriberSeedData() {
    }

    static List<Subscriber> subscribers(int count, String emailDomain) {
        return subscribers(count, emailDomain, LoadtestScenario.REALISTIC, 8);
    }

    static List<Subscriber> subscribers(int count, String emailDomain,
                                        LoadtestScenario scenario, int overloadSendHour) {
        if (count < 0) {
            throw new IllegalArgumentException("subscriber seed count는 0 이상이어야 합니다: " + count);
        }
        if (emailDomain == null || emailDomain.isBlank()) {
            throw new IllegalArgumentException("subscriber seed email domain이 필요합니다.");
        }
        if (scenario == null) {
            throw new IllegalArgumentException("loadtest workload scenario가 필요합니다.");
        }
        if (overloadSendHour < 0 || overloadSendHour > 23) {
            throw new IllegalArgumentException("overload send hour는 0~23이어야 합니다: " + overloadSendHour);
        }

        return IntStream.range(0, count)
                .mapToObj(index -> Subscriber.create(
                        "subscriber-%06d@%s".formatted(index + 1, emailDomain),
                        scenario == LoadtestScenario.OVERLOAD
                                ? overloadSendHour
                                : PREFERRED_SEND_HOURS[index % PREFERRED_SEND_HOURS.length]))
                .toList();
    }

    static int[] preferredSendHours() {
        return PREFERRED_SEND_HOURS.clone();
    }
}
