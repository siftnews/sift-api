package com.siftnews.content.domain;

/**
 * 이슈(뉴스레터 한 호)의 상태 (MVP-DESIGN §2).
 * <p>
 * 선별은 {@link #DRAFT}까지만 만든다 — 발송 예약·전송으로 넘어가는 전이는 M3 발송 범위다.
 */
public enum IssueStatus {
    DRAFT,
    SCHEDULED,
    SENDING,
    SENT
}
