package com.siftnews.subscriber.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SubscribeRequest(
        @NotNull @Positive Long topicId
) {
}
