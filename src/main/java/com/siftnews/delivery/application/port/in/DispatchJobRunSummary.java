package com.siftnews.delivery.application.port.in;

public record DispatchJobRunSummary(
        Long executionId,
        String status,
        String exitCode,
        int createdTaskCount,
        int processedTaskCount,
        int failedTaskCount
) {
}
