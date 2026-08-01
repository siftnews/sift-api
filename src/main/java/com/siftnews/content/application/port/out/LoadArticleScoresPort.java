package com.siftnews.content.application.port.out;

import com.siftnews.content.domain.ArticleScore;

import java.time.Instant;
import java.util.List;

public interface LoadArticleScoresPort {

    /**
     * 토픽의 점수 중 {@code computedAtFrom} 이후에 계산된 것만 돌려준다.
     * <p>
     * 하한이 필요한 이유: {@code article_score}는 {@code (article_id, topic_id)}로 upsert되어
     * <b>과거 기사 점수가 계속 남는다</b>. 하한 없이 읽으면 몇 주 전 기사가 오늘 호에 섞인다.
     * <p>
     * 하한은 <b>선별 윈도우의 {@code from}</b>이다 — 날짜에서 유도하면 존 변환이 끼어들어
     * 트리거의 존과 어긋나는 순간 조회가 통째로 빈다(#35).
     */
    List<ArticleScore> loadByTopic(Long topicId, Instant computedAtFrom);
}
