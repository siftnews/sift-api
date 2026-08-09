package com.siftnews.content.application.port.out;

import com.siftnews.content.domain.Issue;

public interface SaveIssuePort {

    /**
     * 호를 저장하고 id를 돌려준다 — 같은 {@code (topicId, runDate)}가 이미 있으면 <b>항목을 교체</b>한다.
     * <p>
     * 같은 날 같은 토픽으로 다시 돌리는 일은 정상이다(배치 재시도·점수 재계산 후 재생성).
     * 그때 호가 두 개 생기면 구독자에게 같은 날 두 통이 나갈 수 있다.
     */
    Long save(Issue issue);
}
