package com.siftnews.content.domain;

import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * 뉴스레터 한 호 — 한 토픽의 하루치 선별 결과 (MVP-DESIGN §2).
 * <p>
 * {@code (topicId, runDate)}가 사실상의 식별자다. 같은 날 같은 토픽으로 두 번 돌려도 호가
 * 두 개 생기면 안 되므로 영속 계층이 이 쌍에 UNIQUE를 걸고, 재실행은 항목을 교체한다
 * (#21·#25에서 반복된 재실행 멱등성 요구).
 * <p>
 * 선별은 발송 대기 상태인 {@link IssueStatus#SCHEDULED} 이슈를 만든다. 실제 전송 전이는 M3 sendStep 몫이다.
 */
@Getter
public class Issue {

    private final Long issueId;
    private final Long topicId;
    private final LocalDate runDate;
    private final String title;
    private final IssueStatus status;
    private final List<IssueItem> items;

    private Issue(Long issueId, Long topicId, LocalDate runDate, String title,
                  IssueStatus status, List<IssueItem> items) {
        this.issueId = issueId;
        this.topicId = topicId;
        this.runDate = runDate;
        this.title = title;
        this.status = status;
        this.items = items;
    }

    /**
     * 선별 결과로 새 호를 만든다. 항목이 비어 있어도 호는 만든다 — 그날 뽑힌 게 없다는 사실
     * 자체가 기록이고, 호가 아예 없으면 "아직 안 돌았다"와 구분되지 않는다.
     */
    public static Issue draft(Long topicId, LocalDate runDate, String title, List<IssueItem> items) {
        return create(topicId, runDate, title, items, IssueStatus.DRAFT);
    }

    public static Issue scheduled(Long topicId, LocalDate runDate, String title, List<IssueItem> items) {
        return create(topicId, runDate, title, items, IssueStatus.SCHEDULED);
    }

    private static Issue create(Long topicId, LocalDate runDate, String title, List<IssueItem> items,
                                IssueStatus status) {
        if (topicId == null) {
            throw new ContentException("이슈의 topicId는 null일 수 없습니다.");
        }
        if (runDate == null) {
            throw new ContentException("이슈의 runDate는 null일 수 없습니다.");
        }
        if (title == null || title.isBlank()) {
            throw new ContentException("이슈 title은 비어 있을 수 없습니다.");
        }
        return new Issue(null, topicId, runDate, title.strip(), status, requireContiguousRanks(items));
    }

    public static Issue restore(Long issueId, Long topicId, LocalDate runDate, String title,
                                IssueStatus status, List<IssueItem> items) {
        return new Issue(issueId, topicId, runDate, title, status, List.copyOf(items));
    }

    /**
     * rank는 1부터 빠짐없이 이어져야 한다.
     * <p>
     * 렌더링이 rank 순으로 항목을 늘어놓기 때문에, 번호가 건너뛰거나 겹치면 예외 없이
     * <b>뉴스레터에 이상한 순서로 나갈 뿐</b>이라 발견이 늦다. 만들 때 막는다.
     */
    private static List<IssueItem> requireContiguousRanks(List<IssueItem> items) {
        List<IssueItem> copy = List.copyOf(items);
        for (int i = 0; i < copy.size(); i++) {
            int expected = i + 1;
            if (copy.get(i).rank() != expected) {
                throw new ContentException("이슈 항목의 rank는 1부터 연속이어야 합니다: index=" + i
                        + ", rank=" + copy.get(i).rank());
            }
        }
        return copy;
    }
}
