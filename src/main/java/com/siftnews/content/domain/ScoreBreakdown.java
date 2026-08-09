package com.siftnews.content.domain;

import com.siftnews.common.BusinessException;

import java.util.List;

/**
 * 점수 산출 근거 (SELECTION §2.4) — 왜 이 기사가 뽑혔는지를 설명한다.
 * <p>
 * 항목 점수는 전부 0~1로 정규화된 값이고, 최종 점수는 {@link #total()}이 가중합으로 계산한다.
 * <b>이 레코드만으로 최종 점수가 재현되어야 한다</b> — 그래야 저장된 breakdown을 나중에 읽어
 * 가중치 A/B나 회귀 분석을 할 수 있다(SELECTION §5). 그래서 항목 점수뿐 아니라
 * {@link ScoreWeights}와 매칭된 키워드까지 함께 담는다.
 */
public record ScoreBreakdown(
        double keywordScore,
        double recencyScore,
        double trendScore,
        double sourceScore,
        List<String> matchedKeywords,
        ScoreWeights weights) {

    public ScoreBreakdown {
        requireNormalized(keywordScore, "keywordScore");
        requireNormalized(recencyScore, "recencyScore");
        requireNormalized(trendScore, "trendScore");
        requireNormalized(sourceScore, "sourceScore");
        if (weights == null) {
            throw new BusinessException("점수 근거의 weights는 null일 수 없습니다.");
        }
        matchedKeywords = matchedKeywords == null ? List.of() : List.copyOf(matchedKeywords);
    }

    /**
     * 항목 점수는 0~1로 정규화된 값이라는 계약을 만들 때 강제한다.
     * <p>
     * NaN·무한대가 한 항목에 섞이면 {@link #total()}이 통째로 NaN이 되고, 그 기사는 정렬에서
     * 조용히 맨 뒤로 밀린다 — 예외가 나지 않아 발견이 늦다. 범위를 벗어난 값도 마찬가지로
     * 가중치의 의미를 깨뜨리므로 저장 전에 막는다.
     */
    private static void requireNormalized(double value, String name) {
        if (!Double.isFinite(value) || value < 0 || value > 1) {
            throw new BusinessException("점수 항목 " + name + "은 0~1의 유한값이어야 합니다: " + value);
        }
    }

    public double total() {
        return weights.keyword() * keywordScore
                + weights.recency() * recencyScore
                + weights.trend() * trendScore
                + weights.source() * sourceScore;
    }
}
