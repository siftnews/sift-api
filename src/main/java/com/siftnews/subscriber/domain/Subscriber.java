package com.siftnews.subscriber.domain;

import lombok.Getter;

import java.util.Locale;

@Getter
public class Subscriber {

    private final Long subscriberId;
    private final String email;
    private final SubscriberStatus status;
    private final int preferredSendHour;

    private Subscriber(Long subscriberId, String email, SubscriberStatus status, int preferredSendHour) {
        this.subscriberId = subscriberId;
        this.email = email;
        this.status = status;
        this.preferredSendHour = preferredSendHour;
    }

    public static Subscriber create(String email, int preferredSendHour) {
        String normalizedEmail = normalizeEmail(email);
        validateHour(preferredSendHour);
        return new Subscriber(null, normalizedEmail, SubscriberStatus.ACTIVE, preferredSendHour);
    }

    public static Subscriber restore(
            Long subscriberId,
            String email,
            SubscriberStatus status,
            int preferredSendHour
    ) {
        return new Subscriber(subscriberId, email, status, preferredSendHour);
    }

    private static String normalizeEmail(String email) {
        if (email == null
                || email.isBlank()
                || !email.strip().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new SubscriberException("유효한 email이 필요합니다.");
        }

        return email.strip().toLowerCase(Locale.ROOT);
    }

    private static void validateHour(int hour) {
        if (hour < 0 || hour > 23) {
            throw new SubscriberException("preferredSendHour는 0~23이어야 합니다: " + hour);
        }
    }
}
