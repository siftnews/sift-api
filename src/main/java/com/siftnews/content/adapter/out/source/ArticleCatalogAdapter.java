package com.siftnews.content.adapter.out.source;

import com.siftnews.content.application.port.out.LoadCandidateArticlesPort;
import com.siftnews.content.application.port.out.UpdateArticleClusterPort;
import com.siftnews.content.domain.CandidateArticle;
import com.siftnews.source.api.ArticleCandidate;
import com.siftnews.source.api.ArticleCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 선별의 후보 조회·클러스터 갱신을 Source named interface로 내보내는 아웃바운드 어댑터 (D-030).
 * <p>
 * DB가 아니라 <b>다른 모듈</b>이 상대라 {@code adapter/out/source}에 둔다 — content는 article
 * 테이블을 직접 알지 않는다. Source의 {@link ArticleCandidate}를 content의
 * {@link CandidateArticle}로 옮기는 매핑이 한 겹 있는데, 이 한 겹이 두 모듈의 스키마를 떼어 놓는다.
 */
@Component
@RequiredArgsConstructor
class ArticleCatalogAdapter implements LoadCandidateArticlesPort, UpdateArticleClusterPort {

    private final ArticleCatalog articleCatalog;

    @Override
    public List<CandidateArticle> loadCandidates(Instant from, Instant to) {
        return articleCatalog.findCandidates(from, to).stream()
                .map(ArticleCatalogAdapter::toDomain)
                .toList();
    }

    @Override
    public void updateClusters(Map<Long, String> clusterIdsByArticleId) {
        articleCatalog.updateDedupClusters(clusterIdsByArticleId);
    }

    private static CandidateArticle toDomain(ArticleCandidate candidate) {
        return new CandidateArticle(candidate.articleId(), candidate.sourceId(), candidate.normalizedUrl(),
                candidate.title(), candidate.lang(), candidate.body(), candidate.publishedAt(),
                candidate.category(), candidate.dedupClusterId());
    }
}
