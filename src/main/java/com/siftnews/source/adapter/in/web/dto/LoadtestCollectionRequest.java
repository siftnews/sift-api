package com.siftnews.source.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoadtestCollectionRequest(
        @NotBlank
        @Pattern(regexp = "[A-Za-z0-9_-]{1,64}")
        String runId
) {
}
