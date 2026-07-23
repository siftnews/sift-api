package com.siftnews.content.application.port.in;

/**
 * 후보 기사 정규화 컷 + dedup 클러스터링 (선별 1/3, D-030).
 * <p>
 * 인바운드 어댑터(selectionJob의 normalizeDedupStep, M2-5)가 호출한다.
 */
public interface NormalizeDedupUseCase {

    NormalizeDedupSummary normalizeAndDedup();
}
