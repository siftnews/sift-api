package com.siftnews.content.application.port.in;

/**
 * normalizeDedup 한 회 처리 요약 — 측정 베이스라인(하네스 원칙 5) 로깅·검증용.
 */
public record NormalizeDedupSummary(int loaded, int dropped, int clusters) {
}
