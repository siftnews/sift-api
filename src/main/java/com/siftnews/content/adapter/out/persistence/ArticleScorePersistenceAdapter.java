package com.siftnews.content.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siftnews.content.application.port.out.LoadArticleScoresPort;
import com.siftnews.content.application.port.out.SaveArticleScorePort;
import com.siftnews.content.domain.ArticleScore;
import com.siftnews.content.domain.ContentException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@Component
@RequiredArgsConstructor
class ArticleScorePersistenceAdapter implements SaveArticleScorePort, LoadArticleScoresPort {

    /** 8개 bind parameter 기준 PostgreSQL parameter limit을 넘기지 않으며 10,000건을 최대 10회로 묶는다. */
    private static final int SCORE_BATCH_SIZE = 1_000;

    private static final String UPSERT_ARTICLE_SCORES_TEMPLATE = """
            INSERT INTO article_score (article_id, source_id, topic_id, score, breakdown, computed_at, created_at, updated_at)
            VALUES %s
            ON CONFLICT (article_id, topic_id) DO UPDATE SET
                score = EXCLUDED.score,
                breakdown = EXCLUDED.breakdown,
                computed_at = EXCLUDED.computed_at,
                updated_at = EXCLUDED.updated_at
            """;

    private final ArticleScoreJpaRepository articleScoreJpaRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    @Transactional
    public void saveAll(List<ArticleScore> scores) {
        if (scores.isEmpty()) {
            return;
        }

        Timestamp now = Timestamp.from(clock.instant());
        List<ArticleScore> deduplicatedScores = deduplicate(scores);
        for (int from = 0; from < deduplicatedScores.size(); from += SCORE_BATCH_SIZE) {
            int to = Math.min(from + SCORE_BATCH_SIZE, deduplicatedScores.size());
            List<ArticleScore> batch = deduplicatedScores.subList(from, to);
            jdbcTemplate.update(buildUpsertSql(batch.size()), statement -> {
                int parameterIndex = 1;
                for (ArticleScore score : batch) {
                    statement.setLong(parameterIndex++, score.articleId());
                    statement.setLong(parameterIndex++, score.sourceId());
                    statement.setLong(parameterIndex++, score.topicId());
                    statement.setDouble(parameterIndex++, score.score());
                    statement.setString(parameterIndex++, serialize(score));
                    statement.setTimestamp(parameterIndex++, Timestamp.from(score.computedAt()));
                    statement.setTimestamp(parameterIndex++, now);
                    statement.setTimestamp(parameterIndex++, now);
                }
            });
        }
    }

    @Override
    public List<ArticleScore> loadByTopic(Long topicId, Instant computedAtFrom) {
        return articleScoreJpaRepository
                .findByTopicIdAndComputedAtGreaterThanEqual(topicId, computedAtFrom).stream()
                .map(entity -> new ArticleScore(entity.getArticleId(), entity.getSourceId(),
                        entity.getTopicId(), entity.getScore(), entity.getBreakdown(), entity.getComputedAt()))
                .toList();
    }

    private String serialize(ArticleScore score) {
        try {
            return objectMapper.writeValueAsString(score.breakdown());
        } catch (JsonProcessingException e) {
            // 근거를 잃은 점수는 튜닝·설명에 쓸 수 없으므로 조용히 넘기지 않는다.
            throw new ContentException("점수 근거를 JSON으로 변환하지 못했습니다: articleId=" + score.articleId(), e);
        }
    }

    private static List<ArticleScore> deduplicate(List<ArticleScore> scores) {
        Map<ArticleScoreKey, ArticleScore> latestByKey = new LinkedHashMap<>();
        for (ArticleScore score : scores) {
            latestByKey.put(new ArticleScoreKey(score.articleId(), score.topicId()), score);
        }
        return new ArrayList<>(latestByKey.values());
    }

    private static String buildUpsertSql(int batchSize) {
        StringJoiner values = new StringJoiner(",\n");
        for (int index = 0; index < batchSize; index++) {
            values.add("(?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?)");
        }
        return UPSERT_ARTICLE_SCORES_TEMPLATE.formatted(values);
    }

    private record ArticleScoreKey(Long articleId, Long topicId) {
    }
}
