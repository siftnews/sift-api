package com.siftnews.source.application.port.out;

import com.siftnews.source.api.ArticleCandidate;
import com.siftnews.source.api.ArticleExcerpt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface ArticleQueryPort {

    /** {@code [from, to)} — {@code created_at} 기준 반열림 구간 (D-032). */
    List<ArticleCandidate> findCandidates(Instant from, Instant to);

    /** 값이 null이면 클러스터 해제 (D-031). */
    void updateDedupClusters(Map<Long, String> clusterIdsByArticleId);

    List<ArticleExcerpt> findByIds(List<Long> articleIds);
}
