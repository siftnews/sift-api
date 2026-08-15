package com.siftnews.content.adapter.in.web.dto;

import com.siftnews.content.application.port.in.SelectionJobRunSummary;

import java.time.Instant;
import java.time.LocalDate;

public record LoadtestSelectionResponse(
        Long executionId,
        Long topicId,
        LocalDate runDate,
        Instant windowFrom,
        Instant windowTo,
        String status,
        String exitCode,
        long elapsedMs
) {

    public static LoadtestSelectionResponse from(SelectionJobRunSummary summary) {
        return new LoadtestSelectionResponse(summary.executionId(), summary.topicId(), summary.runDate(),
                summary.windowFrom(), summary.windowTo(), summary.status(), summary.exitCode(), summary.elapsedMs());
    }
}
