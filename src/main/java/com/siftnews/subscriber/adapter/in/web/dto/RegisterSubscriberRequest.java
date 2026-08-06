package com.siftnews.subscriber.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RegisterSubscriberRequest(
        @NotNull @Email String email,
        @NotNull @Min(0) @Max(23) Integer preferredSendHour
) {
}
