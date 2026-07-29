package com.siftnews.content.application.port.in;

import java.time.LocalDate;

public interface BuildIssueUseCase {

    /**
     * 한 토픽의 {@code runDate}자 호를 만든다 — 이미 있으면 항목을 교체하고 같은 id를 돌려준다.
     * <p>
     * 점수를 다시 계산하지 않는다. {@code scoreTopic}이 저장해 둔 {@code article_score}를 읽어
     * 컷·랭킹·선택만 한다.
     */
    Long buildIssueForTopic(Long topicId, LocalDate runDate);
}
