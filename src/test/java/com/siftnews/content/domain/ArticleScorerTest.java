package com.siftnews.content.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ArticleScorerTest {

    private static final Instant NOW = Instant.parse("2026-07-26T12:00:00Z");
    private static final int HALF_LIFE_HOURS = 24;

    private static Topic topic(List<String> includeKeywords, Map<String, Double> keywordWeights) {
        return Topic.restore(3L, "개발", "dev", "ko,en", includeKeywords, List.of(),
                keywordWeights, List.of("dev"), HALF_LIFE_HOURS, 10, 0.0, true);
    }

    private static Topic devTopic() {
        return topic(List.of("Spring", "Kafka"), Map.of());
    }

    private static CandidateArticle article(String title, String body, Instant publishedAt) {
        return new CandidateArticle(1L, 7L, "https://ex.com/a", title, "ko", body, publishedAt, "DEV", "c-1");
    }

    private static ArticleScore score(Topic topic, CandidateArticle article, int clusterSize, int maxClusterSize) {
        return ArticleScorer.score(topic, article, clusterSize, maxClusterSize, NOW, ScoreWeights.DEFAULT);
    }

    /** DoD — breakdown만으로 최종 점수가 재현되어야 저장된 근거로 튜닝·회귀 분석을 할 수 있다. */
    @Test
    void totalScoreIsReproducibleFromBreakdownAlone() {
        ArticleScore result = score(devTopic(), article("Spring 배치", "Kafka 정리", NOW), 3, 5);

        assertThat(result.score()).isEqualTo(result.breakdown().total());
    }

    @Test
    void keywordScoreIsOneWhenEveryKeywordMatchesInTitle() {
        ArticleScore result = score(devTopic(), article("Spring 과 Kafka", "본문", NOW), 1, 1);

        assertThat(result.breakdown().keywordScore()).isEqualTo(1.0);
    }

    /** 제목 매치는 본문 매치의 2배 — 같은 키워드라도 제목에 있으면 그 기사의 주제일 확률이 높다. */
    @Test
    void titleMatchCountsTwiceAsMuchAsBodyMatch() {
        double titleOnly = score(devTopic(), article("Spring", "본문", NOW), 1, 1)
                .breakdown().keywordScore();
        double bodyOnly = score(devTopic(), article("제목", "Spring 이야기", NOW), 1, 1)
                .breakdown().keywordScore();

        assertThat(titleOnly).isEqualTo(bodyOnly * 2);
    }

    /** 한 키워드가 제목·본문에 모두 있어도 제목 몫까지만 — 본문 도배가 유리해지면 안 된다. */
    @Test
    void keywordInBothTitleAndBodyCountsOnce() {
        double both = score(devTopic(), article("Spring", "Spring Spring Spring", NOW), 1, 1)
                .breakdown().keywordScore();
        double titleOnly = score(devTopic(), article("Spring", "본문", NOW), 1, 1)
                .breakdown().keywordScore();

        assertThat(both).isEqualTo(titleOnly);
    }

    @Test
    void keywordScoreIsZeroWhenTopicHasNoKeywords() {
        ArticleScore result = score(topic(List.of(), Map.of()), article("아무 제목", "아무 본문", NOW), 1, 1);

        assertThat(result.breakdown().keywordScore()).isZero();
    }

    @Test
    void keywordWeightsShiftRelativeContribution() {
        Topic weighted = topic(List.of("Spring", "Kafka"), Map.of("Spring", 3.0, "Kafka", 1.0));

        double springOnly = ArticleScorer.score(weighted, article("Spring", "본문", NOW), 1, 1, NOW, ScoreWeights.DEFAULT)
                .breakdown().keywordScore();
        double kafkaOnly = ArticleScorer.score(weighted, article("Kafka", "본문", NOW), 1, 1, NOW, ScoreWeights.DEFAULT)
                .breakdown().keywordScore();

        assertThat(springOnly).isEqualTo(kafkaOnly * 3);
    }

    @Test
    void matchedKeywordsAreRecordedForExplanation() {
        ArticleScore result = score(devTopic(), article("Spring 배치", "Kafka 정리", NOW), 1, 1);

        assertThat(result.breakdown().matchedKeywords()).containsExactly("Spring", "Kafka");
    }

    @Test
    void recencyScoreHalvesEveryHalfLife() {
        Instant oneHalfLifeAgo = NOW.minus(java.time.Duration.ofHours(HALF_LIFE_HOURS));

        double fresh = score(devTopic(), article("제목", "본문", NOW), 1, 1).breakdown().recencyScore();
        double aged = score(devTopic(), article("제목", "본문", oneHalfLifeAgo), 1, 1).breakdown().recencyScore();

        assertThat(fresh).isCloseTo(1.0, within(1e-9));
        assertThat(aged).isCloseTo(0.5, within(1e-9));
    }

    /** 발행시각 없는 피드는 의도된 설계 — 탈락이 아니라 최신성 항목에서만 이득을 못 본다. */
    @Test
    void recencyScoreIsZeroWhenPublishedAtMissing() {
        ArticleScore result = score(devTopic(), article("Spring", "본문", null), 1, 1);

        assertThat(result.breakdown().recencyScore()).isZero();
        assertThat(result.score()).isPositive();
    }

    /** 미래 시각을 싣는 피드가 있다 — 나이를 음수로 두면 점수가 1을 넘어 정규화가 깨진다. */
    @Test
    void recencyScoreIsCappedForFuturePublishedAt() {
        Instant future = NOW.plus(java.time.Duration.ofHours(10));

        assertThat(score(devTopic(), article("제목", "본문", future), 1, 1).breakdown().recencyScore())
                .isEqualTo(1.0);
    }

    @Test
    void trendScoreScalesWithClusterSize() {
        double small = score(devTopic(), article("제목", "본문", NOW), 2, 5).breakdown().trendScore();
        double large = score(devTopic(), article("제목", "본문", NOW), 5, 5).breakdown().trendScore();

        assertThat(small).isCloseTo(0.25, within(1e-9));
        assertThat(large).isEqualTo(1.0);
    }

    /**
     * 윈도우에서 아무것도 묶이지 않으면 화제성으로 구분할 정보가 없다 — 전원 만점을 주면
     * 이 항목이 상수가 되어 순위에 기여하지 못한다.
     */
    @Test
    void trendScoreIsZeroWhenNothingClustered() {
        assertThat(score(devTopic(), article("제목", "본문", NOW), 1, 1).breakdown().trendScore()).isZero();
    }

    /** 소스 신뢰도는 아직 근거가 없어 중립값이다 (MVP-DESIGN §6 — trust_score 재검토 대기). */
    @Test
    void sourceScoreIsNeutralUntilTrustScoreExists() {
        assertThat(score(devTopic(), article("제목", "본문", NOW), 1, 1).breakdown().sourceScore())
                .isEqualTo(ArticleScorer.NEUTRAL_SOURCE_SCORE);
    }

    @Test
    void weightsAreRecordedSoPastScoresStayExplainable() {
        ScoreWeights custom = new ScoreWeights(1.0, 0.0, 0.0, 0.0);

        ArticleScore result = ArticleScorer.score(devTopic(), article("Spring 과 Kafka", "본문", NOW),
                1, 1, NOW, custom);

        assertThat(result.breakdown().weights()).isEqualTo(custom);
        assertThat(result.score()).isEqualTo(1.0);
    }

    @Test
    void scoreCarriesTopicAndArticleIdentity() {
        ArticleScore result = score(devTopic(), article("Spring", "본문", NOW), 1, 1);

        assertThat(result.articleId()).isEqualTo(1L);
        assertThat(result.topicId()).isEqualTo(3L);
        assertThat(result.computedAt()).isEqualTo(NOW);
    }
}
