package com.siftnews.content.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Score 단계 (SELECTION §2.4) — 후보 기사에 토픽 관련도 점수를 매긴다.
 * <p>
 * 4항목을 각각 0~1로 정규화한 뒤 {@link ScoreWeights}로 가중합한다. 항목별 값과 가중치는
 * {@link ScoreBreakdown}에 그대로 남겨 "왜 뽑혔나"를 사후에 설명할 수 있게 한다.
 */
public final class ArticleScorer {

    /**
     * 제목 매치는 본문 매치의 2배로 친다 (SELECTION §2.4) — 제목에 있는 키워드가 그 기사의
     * 주제일 확률이 훨씬 높기 때문이다. 한 키워드가 제목·본문에 모두 있어도 2배까지만 쳐서
     * 같은 단어를 본문에 도배한 글이 유리해지지 않게 한다.
     */
    private static final double TITLE_WEIGHT_FACTOR = 2.0;

    private static final double DEFAULT_KEYWORD_WEIGHT = 1.0;

    /**
     * 소스 신뢰도는 아직 상수다.
     * <p>
     * {@code source.trust_score} 컬럼은 M1-4에서 "도메인 미사용"으로 범위 제외되면서
     * <b>M2 스코어링에서 재검토</b>로 남아 있다(MVP-DESIGN §2 주석·§6). 근거 없는 값을
     * 지어내느니 항목 자체는 배선해 두고 값은 중립(1.0)으로 두는 편이, 나중에 실측 근거가
     * 생겼을 때 이 상수만 실제 조회로 바꾸면 되므로 되돌리기 쉽다.
     */
    static final double NEUTRAL_SOURCE_SCORE = 1.0;

    private ArticleScorer() {
    }

    /**
     * @param clusterSize    이 기사가 속한 dedup 클러스터의 크기(자신 포함, 최소 1)
     * @param maxClusterSize 같은 윈도우에서 가장 큰 클러스터의 크기 — 화제성 정규화의 분모
     * @param now            최신성 감쇠의 기준 시각 (주입된 Clock에서 온다)
     */
    public static ArticleScore score(Topic topic, CandidateArticle article,
                                     int clusterSize, int maxClusterSize, Instant now, ScoreWeights weights) {
        List<String> matched = new ArrayList<>();
        double keywordScore = keywordScore(topic, article, matched);

        ScoreBreakdown breakdown = new ScoreBreakdown(
                keywordScore,
                recencyScore(article.publishedAt(), topic.getRecencyHalfLifeHours(), now),
                trendScore(clusterSize, maxClusterSize),
                NEUTRAL_SOURCE_SCORE,
                matched,
                weights);

        return new ArticleScore(article.articleId(), article.sourceId(), topic.getTopicId(),
                breakdown.total(), breakdown, now);
    }

    /**
     * Σ(매칭 키워드 weight × 위치 보정)을 <b>전부 제목에서 맞았을 때의 최댓값</b>으로 나눈다.
     * <p>
     * 키워드 개수로 정규화하므로 키워드를 많이 등록한 토픽이 불리해지지 않는다. 키워드가
     * 없는 토픽은 이 항목의 신호가 없다는 뜻이라 0을 준다(가중치가 0인 것과 같은 효과).
     */
    private static double keywordScore(Topic topic, CandidateArticle article, List<String> matchedOut) {
        List<String> keywords = topic.getIncludeKeywords();
        if (keywords.isEmpty()) {
            return 0.0;
        }

        Map<String, Double> weights = topic.getKeywordWeights();
        double matchedSum = 0.0;
        double maxSum = 0.0;
        for (String keyword : keywords) {
            double weight = weights.getOrDefault(keyword, DEFAULT_KEYWORD_WEIGHT);
            maxSum += weight * TITLE_WEIGHT_FACTOR;

            if (KeywordMatcher.contains(article.title(), keyword)) {
                matchedSum += weight * TITLE_WEIGHT_FACTOR;
                matchedOut.add(keyword);
            } else if (KeywordMatcher.contains(article.body(), keyword)) {
                matchedSum += weight;
                matchedOut.add(keyword);
            }
        }

        // 등록된 키워드 가중치가 전부 0이면 분모가 0이 된다 — 0으로 나누지 않고 신호 없음으로 본다.
        return maxSum == 0.0 ? 0.0 : matchedSum / maxSum;
    }

    /**
     * {@code exp(-ln2 × ageHours / halfLife)} — 반감기마다 절반으로 준다.
     * <p>
     * {@code publishedAt}이 null인 피드가 있다(의도된 설계). 발행 시각을 모르면 최신성 신호도
     * 없는 것이라 0을 준다 — 이 기사가 탈락하는 게 아니라 최신성 항목에서만 이득을 못 볼 뿐이다.
     * 미래 시각을 싣는 피드도 있어 음수 나이는 0으로 눌러 점수가 1을 넘지 않게 한다.
     */
    private static double recencyScore(Instant publishedAt, int halfLifeHours, Instant now) {
        if (publishedAt == null) {
            return 0.0;
        }
        double ageHours = Duration.between(publishedAt, now).toMinutes() / 60.0;
        if (ageHours < 0) {
            ageHours = 0;
        }
        return Math.exp(-Math.log(2) * ageHours / halfLifeHours);
    }

    /**
     * 클러스터가 클수록 여러 매체가 같이 다룬 사건이므로 화제성이 높다.
     * <p>
     * 크기 1(단독 보도)을 0점 기준으로 잡는다 — 윈도우 안에서 아무것도 묶이지 않았다면
     * 화제성으로 구분할 정보가 없다는 뜻이라, 전원에게 만점을 주면 이 항목이 상수가 되어
     * 순위에 기여하지 못한다.
     */
    private static double trendScore(int clusterSize, int maxClusterSize) {
        if (maxClusterSize <= 1) {
            return 0.0;
        }
        return (double) (Math.max(clusterSize, 1) - 1) / (maxClusterSize - 1);
    }
}
