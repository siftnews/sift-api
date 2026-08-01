package com.siftnews.content.application.port.in;

import java.time.Instant;
import java.time.LocalDate;

public interface BuildIssueUseCase {

    /**
     * 한 토픽의 {@code runDate}자 호를 만든다 — 이미 있으면 항목을 교체하고 같은 id를 돌려준다.
     * <p>
     * 점수를 다시 계산하지 않는다. {@code scoreTopic}이 저장해 둔 {@code article_score}를 읽어
     * 컷·랭킹·선택만 한다.
     *
     * @param runDate    호의 식별자 겸 제목에 쓰는 발행일 — <b>조회 범위 계산에는 쓰지 않는다</b>
     * @param scoredFrom 점수 조회 하한. 호출자가 정한 <b>선별 윈도우의 {@code from}</b>을 그대로 넘긴다 —
     *                   스코어링이 계산한 범위와 호가 읽는 범위를 일치시키기 위해서다
     */
    Long buildIssueForTopic(Long topicId, LocalDate runDate, Instant scoredFrom);
}
