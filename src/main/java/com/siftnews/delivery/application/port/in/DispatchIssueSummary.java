package com.siftnews.delivery.application.port.in;

public record DispatchIssueSummary(
        Long deliveryJobId,
        int createdTaskCount
) {
}
