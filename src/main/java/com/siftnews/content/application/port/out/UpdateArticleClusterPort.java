package com.siftnews.content.application.port.out;

/**
 * dedup 클러스터 id 갱신 — article은 Source 소유(D-018)이므로 실 구현은
 * Source named interface의 갱신 오퍼레이션을 호출한다(D-030). 배선은 M2-5.
 */
public interface UpdateArticleClusterPort {

    void updateCluster(Long articleId, String dedupClusterId);
}
