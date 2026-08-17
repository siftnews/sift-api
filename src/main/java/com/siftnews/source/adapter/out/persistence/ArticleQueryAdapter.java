package com.siftnews.source.adapter.out.persistence;

import com.siftnews.source.api.ArticleCandidate;
import com.siftnews.source.api.ArticleExcerpt;
import com.siftnews.source.application.port.out.ArticleQueryPort;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class ArticleQueryAdapter implements ArticleQueryPort {

    private static final int CLUSTER_UPDATE_BATCH_SIZE = 1_000;
    private static final String UPDATE_DEDUP_CLUSTERS_SQL = """
            UPDATE article AS target
            SET dedup_cluster_id = updates.cluster_id
            FROM (VALUES %s) AS updates(article_id, cluster_id)
            WHERE target.id = updates.article_id
            """;

    private final ArticleJpaRepository articleJpaRepository;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<ArticleCandidate> findCandidates(Instant from, Instant to) {
        return articleJpaRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to).stream()
                .map(ArticleQueryAdapter::toCandidate)
                .toList();
    }

    /**
     * 기사별 clusterId 갱신을 1,000건씩 한 번의 UPDATE로 반영한다 — null(해제)도 지원한다.
     * <p>
     * JPA 수정 쿼리의 flush/clear 동작을 유지하면서, 후보 수가 클러스터 수에 비례하는 쿼리
     * 호출로 늘어나지 않게 한다.
     */
    @Override
    @Transactional
    public void updateDedupClusters(Map<Long, String> clusterIdsByArticleId) {
        if (clusterIdsByArticleId == null || clusterIdsByArticleId.isEmpty()) {
            return;
        }

        List<Map.Entry<Long, String>> updates = new ArrayList<>(clusterIdsByArticleId.entrySet());
        entityManager.flush();
        for (int start = 0; start < updates.size(); start += CLUSTER_UPDATE_BATCH_SIZE) {
            int end = Math.min(start + CLUSTER_UPDATE_BATCH_SIZE, updates.size());
            List<Map.Entry<Long, String>> batch = updates.subList(start, end);
            jdbcTemplate.update(buildUpdateSql(batch.size()), statement -> {
                int parameterIndex = 1;
                for (Map.Entry<Long, String> update : batch) {
                    statement.setObject(parameterIndex++, update.getKey(), Types.BIGINT);
                    if (update.getValue() == null) {
                        statement.setNull(parameterIndex++, Types.VARCHAR);
                    } else {
                        statement.setString(parameterIndex++, update.getValue());
                    }
                }
            });
        }
        entityManager.clear();
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

    private static String buildUpdateSql(int batchSize) {
        StringJoiner placeholders = new StringJoiner(", ");
        for (int index = 0; index < batchSize; index++) {
            placeholders.add("(?, CAST(? AS varchar))");
        }
        return UPDATE_DEDUP_CLUSTERS_SQL.formatted(placeholders);
    }
}
