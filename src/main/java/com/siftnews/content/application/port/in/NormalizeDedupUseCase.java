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

    /**
     * @param from 윈도우 시작(포함) — null 불가
     * @param to   윈도우 끝(미포함) — null 불가, {@code from}보다 뒤여야 한다
     * @throws NullPointerException     {@code from}·{@code to}가 null인 경우
     * @throws IllegalArgumentException {@code from}이 {@code to}보다 앞서지 않는 경우.
     *                                  빈 윈도우({@code from == to})도 거부한다 — 후보 0건과
     *                                  구간 계산 실수를 구분하기 위해서다.
     */
    NormalizeDedupSummary normalizeAndDedup(Instant from, Instant to);
}
