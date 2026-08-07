package com.siftnews.subscriber.domain;

public enum SubscriptionStatus {
    ACTIVE,
    PAUSED;

    public boolean isActive() {
        return this == ACTIVE;
    }
}
