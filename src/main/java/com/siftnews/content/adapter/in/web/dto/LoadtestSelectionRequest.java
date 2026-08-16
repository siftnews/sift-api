package com.siftnews.content.adapter.in.web.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.time.LocalDate;

public record LoadtestSelectionRequest(
        @NotNull @Positive Long topicId,
        @NotNull LocalDate runDate,
        @NotNull Instant from,
        @NotNull Instant to
) {

    @AssertTrue(message = "from은 to보다 앞서야 합니다.")
    public boolean isOrderedWindow() {
        return from == null || to == null || from.isBefore(to);
    }
}
