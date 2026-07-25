package com.siftnews.content.application.port.out;

import com.siftnews.content.domain.CandidateArticle;

import java.time.Instant;
import java.util.List;

/**
 * 선별 후보 기사 조회 — 실 구현은 Source named interface를 호출하는 어댑터(D-018·D-030),
 * selectionJob 배치(M2-5)에서 배선한다.
 * <p>
 * 매 실행이 {@code [from, to)} 윈도우 내 후보 <b>전체</b>를 다시 로드한다 — {@code from}은
 * 포함, {@code to}는 미포함인 반열림 구간이다. 이전 실행에서 클러스터에 묶였던 기사도
 * 후보에 다시 포함되어야 컷 탈락 시 상태 해제 대상이 된다(재실행 멱등성의 전제, D-031).
 * 윈도우 크기는 인바운드 어댑터(selectionJob, M2-5)가 결정한다 — 이 포트는 받은 구간을
 * 그대로 조회 조건으로 쓸 뿐이다.
 * <p>
 * 윈도우는 {@code article.created_at}(수집 시각) 기준이다. {@code published_at}은
 * nullable이라 조회 기준으로 쓰면 null인 기사가 영원히 후보에 들지 못하고, 피드
 * 백필·소스 장애 복구로 뒤늦게 수집된 과거 기사도 후보에서 누락된다. {@code created_at}은
 * 단조 증가하고 null이 없어 모든 기사가 정확히 한 번은 후보에 들어온다. (단,
 * {@code publishedAt}은 클러스터 대표 선정 기준으로는 계속 쓰인다 — 윈도우 기준과
 * 대표 선정 기준은 별개다.)
 * <p>
 * 윈도우 불변식 3조항(M2-5가 반드시 지켜야 한다):
 * <ol>
 *   <li>{@code dedup_cluster_id}는 그 기사를 마지막으로 포함한 실행의 윈도우 기준
 *       결과다 — 윈도우가 다른 실행끼리는 값을 비교·집계할 수 없다.</li>
 *   <li>스코어링·다양성 단계의 대상 집합은 직전 normalizeDedupStep 윈도우의
 *       부분집합이어야 한다. 넓히면 분열된 클러스터를 서로 다른 사건으로 오인해
 *       trendScore가 과소 계산되고, MMR 다양성 페널티가 걸리지 않아 같은 사건 기사가
 *       한 뉴스레터에 중복 게재된다.</li>
 *   <li>한 runDate의 모든 selectionJob 실행은 동일한 {@code [from, to)}를 공유해야
 *       한다. selectionJob은 토픽마다 기동되지만(MVP-DESIGN §4 ②) normalizeDedupStep은
 *       토픽 독립 전역 단계라, 토픽별로 윈도우를 다르게 잡으면 나중 토픽이 앞선 토픽의
 *       스코어링 전제를 덮어쓴다.</li>
 * </ol>
 */
public interface LoadCandidateArticlesPort {

    List<CandidateArticle> loadCandidates(Instant from, Instant to);
}
