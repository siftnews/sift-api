package com.siftnews.source.adapter.out.persistence;

import com.siftnews.source.api.ArticleCandidate;
import com.siftnews.source.api.ArticleExcerpt;
import com.siftnews.source.application.port.out.ArticleQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class ArticleQueryAdapter implements ArticleQueryPort {

    private final ArticleJpaRepository articleJpaRepository;

    @Override
    public List<ArticleCandidate> findCandidates(Instant from, Instant to) {
        return articleJpaRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to).stream()
                .map(ArticleQueryAdapter::toCandidate)
                .toList();
    }

    /**
     * 같은 clusterId를 받는 기사끼리 묶어 값별로 한 번씩 UPDATE 한다 — null(해제)도 하나의 그룹이다.
     * <p>
     * {@code HashMap}은 null 키를 허용하므로 해제 그룹을 따로 다룰 필요가 없다.
     */
    @Override
    @Transactional
    public void updateDedupClusters(Map<Long, String> clusterIdsByArticleId) {
        Map<String, List<Long>> idsByClusterId = new HashMap<>();
        for (Map.Entry<Long, String> entry : clusterIdsByArticleId.entrySet()) {
            idsByClusterId.computeIfAbsent(entry.getValue(), key -> new ArrayList<>()).add(entry.getKey());
        }
        idsByClusterId.forEach(articleJpaRepository::updateDedupClusterId);
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
        Map<Long, ArticleExcerpt> foundById = articleJpaRepository.findByIdIn(requestedIds).stream()
                .map(article -> new ArticleExcerpt(article.getId(), article.getTitle(), article.getUrl()))
                .collect(Collectors.toMap(ArticleExcerpt::articleId, Function.identity(), (first, ignored) -> first));
        return requestedIds.stream()
                .map(foundById::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private static ArticleCandidate toCandidate(ArticleJpaEntity entity) {
        return new ArticleCandidate(entity.getId(), entity.getSourceId(), entity.getNormalizedUrl(),
                entity.getTitle(), entity.getLang(), entity.getBody(), entity.getPublishedAt(),
                entity.getCategory() == null ? null : entity.getCategory().name(),
                entity.getDedupClusterId());
    }
}
