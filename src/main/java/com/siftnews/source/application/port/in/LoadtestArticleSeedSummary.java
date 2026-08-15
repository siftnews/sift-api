package com.siftnews.source.application.port.in;

import java.time.Instant;

/**
 * selectionJob용 10,000건 후보 fixture 시드 결과.
 *
 * @param windowFrom 시드 기사만 포함하도록 selectionJob에 넘길 윈도우 하한
 * @param windowTo   {@code windowFrom}부터 고정된 24시간 윈도우의 상한
 */
public record LoadtestArticleSeedSummary(
        String runId,
        int requestedCount,
        int insertedCount,
        int sourceCount,
        Instant windowFrom,
        Instant windowTo
) {
}
