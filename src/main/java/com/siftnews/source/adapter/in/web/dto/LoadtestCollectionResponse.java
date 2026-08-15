package com.siftnews.source.adapter.in.web.dto;

import com.siftnews.source.application.port.in.CollectionJobRunSummary;

public record LoadtestCollectionResponse(
        String runId,
        Long executionId,
        String status,
        String exitCode,
        int sourceReadCount,
        int sourceWriteCount,
        int sourceSkipCount,
        long elapsedMs
) {

    public static LoadtestCollectionResponse from(CollectionJobRunSummary summary) {
        return new LoadtestCollectionResponse(summary.runId(), summary.executionId(), summary.status(), summary.exitCode(),
                summary.sourceReadCount(), summary.sourceWriteCount(), summary.sourceSkipCount(), summary.elapsedMs());
    }
}
