package com.siftnews.content.domain;

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
        matchedKeywords = matchedKeywords == null ? List.of() : List.copyOf(matchedKeywords);
    }

    public double total() {
        return weights.keyword() * keywordScore
                + weights.recency() * recencyScore
                + weights.trend() * trendScore
                + weights.source() * sourceScore;
    }
}
