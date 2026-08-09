package com.siftnews.content.adapter.out.persistence;

import com.siftnews.content.application.port.out.LoadNewsletterIssuePort;
import com.siftnews.content.application.port.out.LoadScheduledIssuesPort;
import com.siftnews.content.application.port.out.NewsletterIssueData;
import com.siftnews.content.application.port.out.NewsletterIssueItemData;
import com.siftnews.content.application.port.out.SaveIssuePort;
import com.siftnews.content.api.ScheduledIssueReference;
import com.siftnews.content.domain.ContentException;
import com.siftnews.content.domain.Issue;
import com.siftnews.content.domain.IssueItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class IssuePersistenceAdapter implements SaveIssuePort, LoadScheduledIssuesPort, LoadNewsletterIssuePort {

    private final IssueJpaRepository issueJpaRepository;
    private final IssueItemJpaRepository issueItemJpaRepository;
    private final Clock clock;

    @Override
    public List<ScheduledIssueReference> loadScheduled(LocalDate runDate) {
        return issueJpaRepository.findScheduled(runDate);
    }

    @Override
    public Optional<NewsletterIssueData> loadNewsletterIssue(Long issueId) {
        return issueJpaRepository.findById(issueId)
                .map(issue -> new NewsletterIssueData(issue.getId(), issue.getTitle(),
                        issueItemJpaRepository.findByIssueIdOrderByRankAsc(issueId).stream()
                                .map(item -> new NewsletterIssueItemData(item.getArticleId(), item.getRank()))
                                .toList()));
    }

    @Override
    @Transactional
    public Long save(Issue issue) {
        issueJpaRepository.upsert(issue.getTopicId(), issue.getRunDate(), issue.getTitle(),
                issue.getStatus().name(), clock.instant());

        IssueJpaEntity saved = issueJpaRepository.findByTopicIdAndRunDate(issue.getTopicId(), issue.getRunDate())
                .orElseThrow(() -> new ContentException("방금 저장한 이슈를 찾을 수 없습니다: topicId="
                        + issue.getTopicId() + ", runDate=" + issue.getRunDate()));

        // 재실행은 게재 목록을 통째로 교체한다 — 남겨 두면 이전 실행의 기사가 그대로 실린다.
        // flush로 삭제를 먼저 반영해야 같은 기사를 다시 넣을 때 UNIQUE(issue_id, article_id)에 걸리지 않는다.
        issueItemJpaRepository.deleteByIssueId(saved.getId());
        issueItemJpaRepository.flush();

        List<IssueItemJpaEntity> items = issue.getItems().stream()
                .map(item -> toEntity(saved.getId(), item))
                .toList();
        issueItemJpaRepository.saveAll(items);

        return saved.getId();
    }

    private static IssueItemJpaEntity toEntity(Long issueId, IssueItem item) {
        return new IssueItemJpaEntity(issueId, item.articleId(), item.rank(), item.score());
    }
}
