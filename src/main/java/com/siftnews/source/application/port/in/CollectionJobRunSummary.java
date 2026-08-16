package com.siftnews.source.application.port.in;

/**
 * collectionJob 한 회 실행 요약 — loadtest HTTP 응답과 배치 측정 로그에 함께 사용한다.
 */
public record CollectionJobRunSummary(
        String runId,
        Long executionId,
        String status,
        String exitCode,
        int sourceReadCount,
        int sourceWriteCount,
        int sourceSkipCount,
        long elapsedMs
) {
}
