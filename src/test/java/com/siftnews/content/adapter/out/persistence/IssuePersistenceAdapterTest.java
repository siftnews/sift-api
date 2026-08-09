package com.siftnews.content.adapter.out.persistence;

import com.siftnews.content.application.port.out.SaveIssuePort;
import com.siftnews.content.domain.Issue;
import com.siftnews.content.domain.IssueItem;
import com.siftnews.content.domain.IssueStatus;
import com.siftnews.support.AbstractIntegrationTest;
import com.siftnews.support.TestDatabaseFixtures;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class IssuePersistenceAdapterTest extends AbstractIntegrationTest {

    private static final Long TOPIC_ID = 3L;
    private static final LocalDate RUN_DATE = LocalDate.parse("2026-07-27");

    @Autowired
    private SaveIssuePort saveIssuePort;

    @Autowired
    private IssueJpaRepository issueJpaRepository;

    @Autowired
    private IssueItemJpaRepository issueItemJpaRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        TestDatabaseFixtures.source(entityManager, 7L);
        TestDatabaseFixtures.topic(entityManager, TOPIC_ID);
        TestDatabaseFixtures.article(entityManager, 11L, 7L);
        TestDatabaseFixtures.article(entityManager, 12L, 7L);
        TestDatabaseFixtures.article(entityManager, 99L, 7L);
    }

    private static Issue issue(List<IssueItem> items) {
        return Issue.draft(TOPIC_ID, RUN_DATE, "개발 2026-07-27", items);
    }

    @Test
    void savesIssueWithItemsInRankOrder() {
        Long issueId = saveIssuePort.save(issue(List.of(
                new IssueItem(11L, 1, 0.9), new IssueItem(12L, 2, 0.7))));

        IssueJpaEntity saved = issueJpaRepository.findById(issueId).orElseThrow();
        assertThat(saved.getTopicId()).isEqualTo(TOPIC_ID);
        assertThat(saved.getRunDate()).isEqualTo(RUN_DATE);
        assertThat(saved.getStatus()).isEqualTo(IssueStatus.DRAFT);
        assertThat(issueItemJpaRepository.findByIssueIdOrderByRankAsc(issueId))
                .extracting(IssueItemJpaEntity::getArticleId)
                .containsExactly(11L, 12L);
    }

    /**
     * 같은 날 같은 토픽을 다시 돌리는 일은 정상이다(배치 재시도·점수 재계산 후 재생성) —
     * 호가 두 개 생기면 구독자에게 같은 날 두 통이 나갈 수 있다.
     */
    @Test
    void rerunReusesSameIssueInsteadOfCreatingAnother() {
        Long first = saveIssuePort.save(issue(List.of(new IssueItem(11L, 1, 0.9))));

        Long second = saveIssuePort.save(issue(List.of(new IssueItem(11L, 1, 0.9))));

        assertThat(second).isEqualTo(first);
        assertThat(issueJpaRepository.findByTopicIdAndRunDate(TOPIC_ID, RUN_DATE)).isPresent();
        assertThat(issueItemJpaRepository.findByIssueIdOrderByRankAsc(first)).hasSize(1);
    }

    @Test
    void rerunPromotesDraftIssueToScheduled() {
        Long issueId = saveIssuePort.save(issue(List.of(new IssueItem(11L, 1, 0.9))));

        saveIssuePort.save(Issue.scheduled(TOPIC_ID, RUN_DATE, "개발 2026-07-27", List.of()));

        entityManager.clear();
        assertThat(issueJpaRepository.findById(issueId).orElseThrow().getStatus()).isEqualTo(IssueStatus.SCHEDULED);
        assertThat(issueJpaRepository.findScheduled(RUN_DATE))
                .extracting(reference -> reference.issueId())
                .containsExactly(issueId);
    }

    /** 재실행은 게재 목록을 통째로 교체한다 — 남겨 두면 이전 실행의 기사가 그대로 실린다. */
    @Test
    void rerunReplacesItemsEntirely() {
        Long issueId = saveIssuePort.save(issue(List.of(
                new IssueItem(11L, 1, 0.9), new IssueItem(12L, 2, 0.7))));

        saveIssuePort.save(issue(List.of(new IssueItem(99L, 1, 0.95))));

        assertThat(issueItemJpaRepository.findByIssueIdOrderByRankAsc(issueId))
                .extracting(IssueItemJpaEntity::getArticleId)
                .containsExactly(99L);
    }

    @Test
    void savesIssueWithNoItems() {
        Long issueId = saveIssuePort.save(issue(List.of()));

        assertThat(issueJpaRepository.findById(issueId)).isPresent();
        assertThat(issueItemJpaRepository.findByIssueIdOrderByRankAsc(issueId)).isEmpty();
    }
}
