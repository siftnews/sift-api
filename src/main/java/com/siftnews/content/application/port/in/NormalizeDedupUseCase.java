package com.siftnews.content.application.port.in;

import java.time.Instant;

/**
 * 후보 기사 정규화 컷 + dedup 클러스터링 (선별 1/3, D-030).
 * <p>
 * 인바운드 어댑터(selectionJob의 normalizeDedupStep, M2-5)가 호출한다. 호출자가
 * 후보 로드 윈도우 {@code [from, to)}를 결정한다 — 서비스는 스스로 윈도우를
 * 만들지 않는다(D-031).
 */
public interface NormalizeDedupUseCase {

    NormalizeDedupSummary normalizeAndDedup(Instant from, Instant to);
}
