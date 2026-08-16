package com.siftnews.content.application.port.in;

import java.time.Instant;
import java.time.LocalDate;

/**
 * selectionJob 한 회 실행 요약 — loadtest HTTP 응답과 배치 측정 결과에 함께 사용한다.
 */
public record SelectionJobRunSummary(
        Long executionId,
        Long topicId,
        LocalDate runDate,
        Instant windowFrom,
        Instant windowTo,
        String status,
        String exitCode,
        long elapsedMs
) {
}
