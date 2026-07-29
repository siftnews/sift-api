package com.siftnews.source.api;

import java.time.Instant;
import java.util.Map;

/**
 * 선별이 쓰는 기사 조회·클러스터 갱신 오퍼레이션 (D-030).
 */
public interface ArticleCatalog {

    /**
     * {@code [from, to)} 윈도우의 기사를 전부 돌려준다 — {@code from} 포함, {@code to} 미포함.
     * <p>
     * 기준 컬럼은 {@code article.created_at}(수집 시각)이다. {@code published_at}은 nullable이라
     * 조회 기준으로 쓰면 null인 기사가 영원히 후보에 들지 못하고, 뒤늦게 수집된 과거 기사도
     * 누락된다 (D-032).
     */
    java.util.List<ArticleCandidate> findCandidates(Instant from, Instant to);

    /**
     * dedup 클러스터 id를 벌크로 교체한다 — <b>값이 null이면 해제</b>다.
     * <p>
     * 선별은 매 실행이 윈도우 후보 전체의 상태를 통째로 바꾸므로(D-031), 부여와 해제가 한 번의
     * 호출에 섞여 들어온다. 건별 호출로 쪼개면 실행 도중 중단 시 일부만 반영된 상태가 남는다.
     */
    void updateDedupClusters(Map<Long, String> clusterIdsByArticleId);
}
