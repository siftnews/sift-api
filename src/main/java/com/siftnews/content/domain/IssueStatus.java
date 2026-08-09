package com.siftnews.content.domain;

/**
 * 이슈(뉴스레터 한 호)의 상태 (MVP-DESIGN §2).
 * <p>
 * 선별은 {@link #SCHEDULED}까지 만들고, 전송·완료 전이는 M3 sendStep·retryJob 범위다.
 */
public enum IssueStatus {
    DRAFT,
    SCHEDULED,
    SENDING,
    SENT
}
