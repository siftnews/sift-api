package com.siftnews.source.application.service;

import com.siftnews.source.api.ArticleCandidate;
import com.siftnews.source.api.ArticleCatalog;
import com.siftnews.source.api.ArticleExcerpt;
import com.siftnews.source.application.port.out.ArticleQueryPort;
import com.siftnews.source.domain.ArticleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * named interface {@link ArticleCatalog}의 구현 — 다른 모듈의 호출을 받는 인바운드 유스케이스다.
 * <p>
 * 얇지만 영속 어댑터가 직접 named interface를 구현하지 않게 막는 자리다. 어댑터가 port.in을
 * 구현하면 의존 방향이 뒤집힌다(PR #24에서 시더가 같은 이유로 지적받았다).
 */
@Service
@RequiredArgsConstructor
public class ArticleCatalogService implements ArticleCatalog {

    private final ArticleQueryPort articleQueryPort;

    @Override
    public List<ArticleCandidate> findCandidates(Instant from, Instant to) {
        if (from == null || to == null) {
            throw new ArticleException("후보 조회 윈도우는 null일 수 없습니다: from=" + from + ", to=" + to);
        }
        if (!from.isBefore(to)) {
            throw new ArticleException("from은 to보다 앞서야 합니다: from=" + from + ", to=" + to);
        }
        return articleQueryPort.findCandidates(from, to);
    }

    @Override
    public List<ArticleExcerpt> findByIds(List<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return List.of();
        }
        List<Long> requestedIds = articleIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (requestedIds.isEmpty()) {
            return List.of();
        }
        Map<Long, ArticleExcerpt> foundById = articleQueryPort.findByIds(requestedIds).stream()
                .collect(Collectors.toMap(ArticleExcerpt::articleId, Function.identity(), (first, ignored) -> first));
        return requestedIds.stream()
                .map(foundById::get)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public void updateDedupClusters(Map<Long, String> clusterIdsByArticleId) {
        // 빈 맵은 불필요한 DB 갱신을 만들지 않도록 조기 반환한다.
        if (clusterIdsByArticleId == null || clusterIdsByArticleId.isEmpty()) {
            return;
        }
        articleQueryPort.updateDedupClusters(clusterIdsByArticleId);
    }
}
