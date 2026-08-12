package com.siftnews.delivery.adapter.in.web.dto;

import com.siftnews.delivery.application.port.in.DispatchJobRunSummary;

public record LoadtestDispatchResponse(
        Long issueId,
        Long topicId,
        int preferredSendHour,
        Long executionId,
        String status,
        String exitCode,
        int createdTaskCount,
        int processedTaskCount,
        int failedTaskCount
) {

    public static LoadtestDispatchResponse from(LoadtestDispatchRequest request,
                                                DispatchJobRunSummary summary) {
        return new LoadtestDispatchResponse(
                request.issueId(),
                request.topicId(),
                request.preferredSendHour(),
                summary.executionId(),
                summary.status(),
                summary.exitCode(),
                summary.createdTaskCount(),
                summary.processedTaskCount(),
                summary.failedTaskCount());
    }
}
