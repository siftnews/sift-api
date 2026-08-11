package com.siftnews.subscriber.adapter.in.bootstrap;

import java.util.Locale;

enum LoadtestScenario {
    REALISTIC,
    OVERLOAD;

    static LoadtestScenario parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("loadtest workload scenario가 필요합니다.");
        }
        String normalized = value.strip().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "REALISTIC" -> REALISTIC;
            case "OVERLOAD" -> OVERLOAD;
            default -> throw new IllegalArgumentException(
                    "지원하지 않는 loadtest workload scenario입니다: " + value);
        };
    }
}
