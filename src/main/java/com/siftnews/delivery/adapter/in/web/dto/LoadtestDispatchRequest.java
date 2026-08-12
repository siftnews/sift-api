package com.siftnews.delivery.adapter.in.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record LoadtestDispatchRequest(
        @NotNull @Positive Long issueId,
        @NotNull @Positive Long topicId,
        @NotNull @Min(0) @Max(23) Integer preferredSendHour
) {
}
