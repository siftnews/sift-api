package com.siftnews.content.adapter.out.persistence;

import com.siftnews.content.application.port.out.SaveArticleScorePort;
import com.siftnews.content.domain.ArticleScore;
import com.siftnews.content.domain.ScoreBreakdown;
import com.siftnews.content.domain.ScoreWeights;
import com.siftnews.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ArticleScorePersistenceAdapterTest extends AbstractIntegrationTest {

    private static final Long TOPIC_ID = 3L;
    private static final Instant COMPUTED_AT = Instant.parse("2026-07-26T12:00:00Z");

    @Autowired
    private SaveArticleScorePort saveArticleScorePort;

    @Autowired
    private ArticleScoreJpaRepository articleScoreJpaRepository;

    private static final Long SOURCE_ID = 7L;

    private static ArticleScore score(long articleId, double value) {
        ScoreBreakdown breakdown = new ScoreBreakdown(value, 0.5, 0.25, 1.0,
                List.of("Spring", "Kafka"), ScoreWeights.DEFAULT);
        return new ArticleScore(articleId, SOURCE_ID, TOPIC_ID, breakdown.total(), breakdown, COMPUTED_AT);
    }

    /** breakdown이 JSON으로 저장됐다가 그대로 돌아와야 나중에 튜닝·회귀 분석에 쓸 수 있다. */
    @Test
    void savesBreakdownAsJsonAndReadsItBack() {
        ArticleScore saved = score(11L, 0.8);

        saveArticleScorePort.saveAll(List.of(saved));

        List<ArticleScoreJpaEntity> found = articleScoreJpaRepository.findByTopicId(TOPIC_ID);
        assertThat(found).hasSize(1);
        ArticleScoreJpaEntity entity = found.get(0);
        assertThat(entity.getArticleId()).isEqualTo(11L);
        assertThat(entity.getScore()).isEqualTo(saved.score());
        assertThat(entity.getComputedAt()).isEqualTo(COMPUTED_AT);
        assertThat(entity.getBreakdown()).isEqualTo(saved.breakdown());
        assertThat(entity.getBreakdown().matchedKeywords()).containsExactly("Spring", "Kafka");
        assertThat(entity.getBreakdown().weights()).isEqualTo(ScoreWeights.DEFAULT);
    }

    /**
     * 같은 윈도우 재실행은 정상이다(배치 재시도·수동 재실행) — 행이 쌓이면 Rank &amp; Select가
     * 한 기사를 여러 번 보게 되므로, 두 번째 저장은 덮어써야 한다.
     */
    @Test
    void rerunUpdatesInsteadOfInsertingDuplicate() {
        saveArticleScorePort.saveAll(List.of(score(11L, 0.2)));
        ArticleScore rescored = score(11L, 0.9);

        saveArticleScorePort.saveAll(List.of(rescored));

        List<ArticleScoreJpaEntity> found = articleScoreJpaRepository.findByTopicId(TOPIC_ID);
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getScore()).isEqualTo(rescored.score());
        assertThat(found.get(0).getBreakdown().keywordScore()).isEqualTo(0.9);
    }

    @Test
    void savesEachArticleTopicPairSeparately() {
        saveArticleScorePort.saveAll(List.of(score(11L, 0.2), score(12L, 0.4)));

        assertThat(articleScoreJpaRepository.findByTopicId(TOPIC_ID))
                .extracting(ArticleScoreJpaEntity::getArticleId)
                .containsExactlyInAnyOrder(11L, 12L);
    }
}
