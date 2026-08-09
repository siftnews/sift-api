package com.siftnews.content.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IssueTest {

    private static final LocalDate RUN_DATE = LocalDate.parse("2026-07-27");

    @Test
    void draftStartsInDraftStatus() {
        Issue issue = Issue.draft(3L, RUN_DATE, "개발 2026-07-27",
                List.of(new IssueItem(1L, 1, 0.9), new IssueItem(2L, 2, 0.8)));

        assertThat(issue.getStatus()).isEqualTo(IssueStatus.DRAFT);
        assertThat(issue.getIssueId()).isNull();
        assertThat(issue.getItems()).hasSize(2);
    }

    /**
     * 뽑힌 게 없어도 호는 만든다 — 그날 결과가 없다는 사실 자체가 기록이고,
     * 호가 아예 없으면 "아직 안 돌았다"와 구분되지 않는다.
     */
    @Test
    void draftAllowsEmptyItems() {
        assertThat(Issue.draft(3L, RUN_DATE, "개발 2026-07-27", List.of()).getItems()).isEmpty();
    }

    /**
     * 렌더링이 rank 순으로 늘어놓기 때문에, 번호가 건너뛰면 예외 없이 이상한 순서로
     * 나갈 뿐이라 발견이 늦다 — 만들 때 막는다.
     */
    @Test
    void rejectsNonContiguousRanks() {
        assertThatThrownBy(() -> Issue.draft(3L, RUN_DATE, "제목",
                List.of(new IssueItem(1L, 1, 0.9), new IssueItem(2L, 3, 0.8))))
                .isInstanceOf(ContentException.class);
    }

    @Test
    void rejectsRanksNotStartingAtOne() {
        assertThatThrownBy(() -> Issue.draft(3L, RUN_DATE, "제목", List.of(new IssueItem(1L, 2, 0.9))))
                .isInstanceOf(ContentException.class);
    }

    @Test
    void rejectsBlankTitle() {
        assertThatThrownBy(() -> Issue.draft(3L, RUN_DATE, "  ", List.of()))
                .isInstanceOf(ContentException.class);
    }

    @Test
    void rejectsMissingRunDate() {
        assertThatThrownBy(() -> Issue.draft(3L, null, "제목", List.of()))
                .isInstanceOf(ContentException.class);
    }

    @Test
    void itemRankMustBePositive() {
        assertThatThrownBy(() -> new IssueItem(1L, 0, 0.5))
                .isInstanceOf(RuntimeException.class);
    }
}
