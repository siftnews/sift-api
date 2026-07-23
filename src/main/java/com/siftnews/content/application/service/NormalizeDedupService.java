package com.siftnews.content.application.service;

import com.siftnews.content.application.port.in.NormalizeDedupSummary;
import com.siftnews.content.application.port.in.NormalizeDedupUseCase;
import com.siftnews.content.application.port.out.LoadCandidateArticlesPort;
import com.siftnews.content.application.port.out.UpdateArticleClusterPort;
import com.siftnews.content.domain.ArticleCluster;
import com.siftnews.content.domain.ArticleNormalizer;
import com.siftnews.content.domain.CandidateArticle;
import com.siftnews.content.domain.DedupClusterer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 선별 1/3 — 후보 로드 → 정규화 컷 → Jaccard 클러스터링 → 클러스터 id 갱신 (D-030).
 * <p>
 * 컷 탈락 기사는 클러스터를 부여하지 않는다(이후 필터·스코어링에서 제외). 대표 기사만
 * 스코어링 대상이 되는 로직은 후속(Filter/Score) 단계 소관.
 */
@Slf4j
@RequiredArgsConstructor
public class NormalizeDedupService implements NormalizeDedupUseCase {

    private static final double JACCARD_THRESHOLD = 0.7;

    private final LoadCandidateArticlesPort loadCandidateArticlesPort;
    private final UpdateArticleClusterPort updateArticleClusterPort;

    @Override
    public NormalizeDedupSummary normalizeAndDedup() {
        List<CandidateArticle> loaded = loadCandidateArticlesPort.loadCandidates();
        List<CandidateArticle> survivors = loaded.stream()
                .filter(ArticleNormalizer::survives)
                .toList();
        int dropped = loaded.size() - survivors.size();

        List<ArticleCluster> clusters = DedupClusterer.cluster(survivors, JACCARD_THRESHOLD);
        for (ArticleCluster cluster : clusters) {
            for (Long articleId : cluster.memberIds()) {
                updateArticleClusterPort.updateCluster(articleId, cluster.clusterId());
            }
        }

        log.info("normalizeDedup 완료: loaded={}, dropped={}, clusters={}", loaded.size(), dropped, clusters.size());
        return new NormalizeDedupSummary(loaded.size(), dropped, clusters.size());
    }
}
