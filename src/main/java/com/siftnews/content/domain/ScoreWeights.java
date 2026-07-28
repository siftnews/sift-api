package com.siftnews.content.domain;

import com.siftnews.common.BusinessException;

/**
 * 스코어 항목별 가중치 (SELECTION §2.4).
 * <p>
 * breakdown에 <b>함께 저장한다</b> — 가중치를 나중에 튜닝하면 과거 점수를 재현할 수 없게 되는데,
 * 당시 가중치를 같이 남겨 두면 "이 점수는 그때 그 가중치로 이렇게 나왔다"가 계속 설명된다.
 */
public record ScoreWeights(double keyword, double recency, double trend, double source) {

    /** SELECTION §2.4 초안값 — 실측 후 조정 대상이다(SELECTION §6 열린 질문). */
    public static final ScoreWeights DEFAULT = new ScoreWeights(0.5, 0.2, 0.2, 0.1);

    public ScoreWeights {
        requireFinite(keyword, "keyword");
        requireFinite(recency, "recency");
        requireFinite(trend, "trend");
        requireFinite(source, "source");
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value) || value < 0) {
            throw new BusinessException("가중치 " + name + "는 0 이상의 유한값이어야 합니다: " + value);
        }
    }
}
