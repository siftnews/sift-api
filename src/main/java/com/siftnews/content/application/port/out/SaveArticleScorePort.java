package com.siftnews.content.application.port.out;

import com.siftnews.content.domain.ArticleScore;

import java.util.List;

public interface SaveArticleScorePort {

    /**
     * 토픽 × 기사 점수를 저장한다 — 같은 쌍이 이미 있으면 <b>덮어쓴다</b>.
     * <p>
     * 같은 윈도우를 다시 돌리는 일은 정상이다(배치 재시도·수동 재실행). 그때마다 행이
     * 쌓이면 Rank &amp; Select가 한 기사를 여러 번 보게 되므로, 재실행은 결과를 교체해야 한다
     * (D-031에서 normalizeDedup이 겪은 것과 같은 멱등성 요구).
     */
    void saveAll(List<ArticleScore> scores);
}
