package com.siftnews.content.application.service;

import com.siftnews.content.application.port.in.NormalizeDedupSummary;
import com.siftnews.content.application.port.in.NormalizeDedupUseCase;
import com.siftnews.content.application.port.out.LoadCandidateArticlesPort;
import com.siftnews.content.application.port.out.UpdateArticleClusterPort;
import com.siftnews.content.domain.ArticleCluster;
import com.siftnews.content.domain.ArticleNormalizer;
import com.siftnews.content.domain.CandidateArticle;
import com.siftnews.content.domain.ContentException;
import com.siftnews.content.domain.DedupClusterer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * 선별 1/3 — 후보 로드 → 정규화 컷 → Jaccard 클러스터링 → 클러스터 id 벌크 갱신 (D-030·D-031).
 * <p>
 * 매 실행이 윈도우 내 후보 전체를 다시 로드해 상태를 통째로 교체한다 — 컷 탈락 기사는
 * 클러스터를 부여하지 않을 뿐 아니라, 이전 실행에서 부여받았을 clusterId를 null로
 * 명시적으로 해제한다. 그래야 재실행마다 컷 결과가 정확히 반영되는 멱등성이 보장된다
 * (D-031 — "신규만" 로드하면 이전 실행에 묶인 기사가 후보에 없어 되돌릴 대상 자체가 없다).
 * 대표 기사만 스코어링 대상이 되는 로직은 후속(Filter/Score) 단계 소관.
 */
@Slf4j
@RequiredArgsConstructor
public class NormalizeDedupService implements NormalizeDedupUseCase {

    private static final double JACCARD_THRESHOLD = 0.7;

    private final LoadCandidateArticlesPort loadCandidateArticlesPort;
    private final UpdateArticleClusterPort updateArticleClusterPort;

    @Override
    public NormalizeDedupSummary normalizeAndDedup(Instant from, Instant to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (!from.isBefore(to)) {
            throw new ContentException("from은 to보다 앞서야 한다: from=" + from + ", to=" + to);
        }

        List<CandidateArticle> loaded = loadCandidateArticlesPort.loadCandidates(from, to);
        List<CandidateArticle> survivors = loaded.stream()
                .filter(ArticleNormalizer::survives)
                .toList();
        int dropped = loaded.size() - survivors.size();

        List<ArticleCluster> clusters = DedupClusterer.cluster(survivors, JACCARD_THRESHOLD);

        // 실행 단위 상태 교체: 로드한 후보 전체를 우선 null로 채운 뒤, 클러스터에 속한
        // 기사만 실제 clusterId로 덮어쓴다 — 컷 탈락 기사는 null(해제)로 남는다.
        // TreeMap으로 articleId 오름차순을 고정해 어댑터의 갱신 순서를 재실행마다 재현 가능하게 한다.
        Map<Long, String> clusterIdsByArticleId = new TreeMap<>();
        for (CandidateArticle article : loaded) {
            clusterIdsByArticleId.put(article.articleId(), null);
        }
        for (ArticleCluster cluster : clusters) {
            for (Long articleId : cluster.memberIds()) {
                clusterIdsByArticleId.put(articleId, cluster.clusterId());
            }
        }

        // 후보 0건은 정상 상황(첫 기동·크롤 실패 직후·좁은 윈도우) — 빈 맵으로 포트를 부르면
        // 어댑터의 `IN ()` 구현이 SQL 문법 오류를 낼 수 있어 조기 반환한다.
        if (clusterIdsByArticleId.isEmpty()) {
            log.info("normalizeDedup 완료: loaded=0, dropped=0, clusters=0 (후보 없음, 포트 호출 생략)");
            return new NormalizeDedupSummary(0, 0, 0);
        }

        updateArticleClusterPort.updateClusters(clusterIdsByArticleId);

        log.info("normalizeDedup 완료: loaded={}, dropped={}, clusters={}", loaded.size(), dropped, clusters.size());
        return new NormalizeDedupSummary(loaded.size(), dropped, clusters.size());
    }
}
