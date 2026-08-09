package com.siftnews.content.application.port.in;

import java.time.Instant;

public interface ScoreArticlesUseCase {

    /**
     * 한 토픽에 대해 {@code [from, to)} 윈도우의 후보를 걸러 점수를 매기고 저장한다.
     * <p>
     * 윈도우는 <b>직전 normalizeDedup 실행과 같은 구간이어야 한다</b> — 넓히면 그 밖의 기사가
     * 옛 클러스터 id를 단 채 섞여 화제성이 틀어진다 (D-032 불변식 2).
     */
    ScoreArticlesSummary scoreTopic(Long topicId, Instant from, Instant to);
}
