package com.siftnews.source.adapter.in.web.dto;

import com.siftnews.source.application.port.in.LoadtestArticleSeedSummary;

import java.time.Instant;

public record LoadtestSelectionSeedResponse(
        String runId,
        int requestedCount,
        int insertedCount,
        int sourceCount,
        Instant windowFrom,
        Instant windowTo
) {

    public static LoadtestSelectionSeedResponse from(LoadtestArticleSeedSummary summary) {
        return new LoadtestSelectionSeedResponse(summary.runId(), summary.requestedCount(),
                summary.insertedCount(), summary.sourceCount(), summary.windowFrom(), summary.windowTo());
    }
}
