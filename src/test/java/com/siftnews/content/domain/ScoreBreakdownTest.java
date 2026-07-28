package com.siftnews.content.domain;

import com.siftnews.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class ScoreBreakdownTest {

    private static ScoreBreakdown breakdown(double keywordScore) {
        return new ScoreBreakdown(keywordScore, 0.5, 0.25, 1.0, List.of("Spring"), ScoreWeights.DEFAULT);
    }

    @Test
    void totalIsWeightedSumOfComponents() {
        // 0.5×1.0 + 0.2×0.5 + 0.2×0.25 + 0.1×1.0
        assertThat(breakdown(1.0).total()).isCloseTo(0.75, within(1e-9));
    }

    /**
     * NaN이 한 항목에 섞이면 {@code total()}이 통째로 NaN이 되고 그 기사는 정렬에서 조용히
     * 맨 뒤로 밀린다 — 예외가 나지 않아 발견이 늦으므로 만들 때 막는다.
     */
    @Test
    void rejectsNaNComponent() {
        assertThatThrownBy(() -> breakdown(Double.NaN)).isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsInfiniteComponent() {
        assertThatThrownBy(() -> breakdown(Double.POSITIVE_INFINITY)).isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsComponentOutsideNormalizedRange() {
        assertThatThrownBy(() -> breakdown(1.5)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> breakdown(-0.1)).isInstanceOf(BusinessException.class);
    }

    /** weights가 없으면 total()에서 NPE가 난다 — 근거만으로 점수를 재현한다는 계약이 깨진다. */
    @Test
    void rejectsMissingWeights() {
        assertThatThrownBy(() -> new ScoreBreakdown(0.5, 0.5, 0.5, 0.5, List.of(), null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void allowsBoundaryValues() {
        assertThat(breakdown(0.0)).isNotNull();
        assertThat(breakdown(1.0)).isNotNull();
    }

    @Test
    void nullMatchedKeywordsBecomesEmptyList() {
        assertThat(new ScoreBreakdown(0.5, 0.5, 0.5, 0.5, null, ScoreWeights.DEFAULT).matchedKeywords()).isEmpty();
    }
}
