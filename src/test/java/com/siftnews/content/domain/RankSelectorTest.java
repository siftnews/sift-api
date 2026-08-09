package com.siftnews.content.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RankSelectorTest {

    private static final Instant COMPUTED_AT = Instant.parse("2026-07-27T00:00:00Z");

    private static Topic topic(double scoreThreshold, int maxItems) {
        return Topic.restore(3L, "개발", "dev", "ko,en", List.of("Spring"), List.of(),
                Map.of(), List.of("dev"), 24, maxItems, scoreThreshold, true);
    }

    private static ArticleScore score(long articleId, long sourceId, double value) {
        ScoreBreakdown breakdown = new ScoreBreakdown(value, 0, 0, 0, List.of(), ScoreWeights.DEFAULT);
        return new ArticleScore(articleId, sourceId, 3L, value, breakdown, COMPUTED_AT);
    }

    @Test
    void ranksByScoreDescendingStartingAtOne() {
        List<IssueItem> items = RankSelector.select(topic(0.0, 10), List.of(
                score(1L, 10L, 0.3), score(2L, 20L, 0.9), score(3L, 30L, 0.6)));

        assertThat(items).extracting(IssueItem::articleId).containsExactly(2L, 3L, 1L);
        assertThat(items).extracting(IssueItem::rank).containsExactly(1, 2, 3);
    }

    @Test
    void dropsScoresBelowThreshold() {
        List<IssueItem> items = RankSelector.select(topic(0.5, 10), List.of(
                score(1L, 10L, 0.4), score(2L, 20L, 0.5), score(3L, 30L, 0.9)));

        assertThat(items).extracting(IssueItem::articleId).containsExactly(3L, 2L);
    }

    @Test
    void takesAtMostMaxItems() {
        List<IssueItem> items = RankSelector.select(topic(0.0, 2), List.of(
                score(1L, 10L, 0.9), score(2L, 20L, 0.8), score(3L, 30L, 0.7)));

        assertThat(items).hasSize(2);
    }

    /**
     * 한 소스가 상위를 독식하면 다른 소스에 자리를 내준다 — 점수만 보면 1·2위가 같은 소스지만
     * 감점 후에는 다른 소스가 2위로 올라온다.
     */
    @Test
    void penalizesRepeatedSourceSoOthersGetIn() {
        List<IssueItem> items = RankSelector.select(topic(0.0, 2), List.of(
                score(1L, 10L, 1.0), score(2L, 10L, 0.9), score(3L, 20L, 0.8)));

        assertThat(items).extracting(IssueItem::articleId).containsExactly(1L, 3L);
    }

    /**
     * 감점이지 금지가 아니다 — 점수 차가 충분히 크면 같은 소스라도 이긴다. 하드 상한을 두면
     * 좋은 기사가 한 매체에 몰린 날 빈자리를 낮은 점수로 채우게 된다.
     */
    @Test
    void sameSourceStillWinsWhenScoreGapIsLargeEnough() {
        List<IssueItem> items = RankSelector.select(topic(0.0, 2), List.of(
                score(1L, 10L, 1.0), score(2L, 10L, 0.9), score(3L, 20L, 0.5)));

        // 0.9 × 0.7 = 0.63 > 0.5 이므로 같은 소스가 2위를 지킨다
        assertThat(items).extracting(IssueItem::articleId).containsExactly(1L, 2L);
    }

    /** 게재 점수는 감점 전 원점수다 — 감점은 순서를 정하는 데만 쓴다. */
    @Test
    void recordsOriginalScoreNotPenalized() {
        List<IssueItem> items = RankSelector.select(topic(0.0, 2), List.of(
                score(1L, 10L, 1.0), score(2L, 10L, 0.9)));

        assertThat(items.get(1).score()).isEqualTo(0.9);
    }

    /** 동점이면 articleId 오름차순 — 순서가 흔들리면 같은 입력에 다른 호가 나온다. */
    @Test
    void breaksTiesByArticleIdForDeterminism() {
        List<IssueItem> items = RankSelector.select(topic(0.0, 10), List.of(
                score(9L, 90L, 0.5), score(2L, 20L, 0.5), score(5L, 50L, 0.5)));

        assertThat(items).extracting(IssueItem::articleId).containsExactly(2L, 5L, 9L);
    }

    @Test
    void returnsEmptyWhenNothingPassesThreshold() {
        assertThat(RankSelector.select(topic(0.9, 10), List.of(score(1L, 10L, 0.2)))).isEmpty();
    }
}
