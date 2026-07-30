package com.siftnews.content.application.service;

import com.siftnews.content.application.port.in.BuildIssueUseCase;
import com.siftnews.content.application.port.out.LoadArticleScoresPort;
import com.siftnews.content.application.port.out.LoadTopicPort;
import com.siftnews.content.application.port.out.SaveIssuePort;
import com.siftnews.content.domain.ArticleScore;
import com.siftnews.content.domain.ContentException;
import com.siftnews.content.domain.Issue;
import com.siftnews.content.domain.IssueItem;
import com.siftnews.content.domain.RankSelector;
import com.siftnews.content.domain.Topic;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

/**
 * 선별 3/3 — 점수 로드 → threshold 컷·랭킹·소스 다양성 → 상위 {@code maxItems}건으로 호 생성 (SELECTION §2.5).
 * <p>
 * 점수는 다시 계산하지 않는다 — {@code scoreTopic}(선별 2/3)이 저장한 {@code article_score}를 읽는다.
 * <p>
 * 로드 하한을 {@code runDate}의 UTC 자정으로 잡는다. {@code article_score}는 기사·토픽 쌍으로
 * upsert되어 과거 점수가 계속 남으므로, 하한이 없으면 몇 주 전 기사가 오늘 호에 섞인다.
 * 발행은 하루 한 번이므로(D-019) 그날 계산된 점수 = 이번 실행분이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BuildIssueService implements BuildIssueUseCase {

    private final LoadTopicPort loadTopicPort;
    private final LoadArticleScoresPort loadArticleScoresPort;
    private final SaveIssuePort saveIssuePort;

    @Override
    public Long buildIssueForTopic(Long topicId, LocalDate runDate) {
        Objects.requireNonNull(topicId, "topicId");
        Objects.requireNonNull(runDate, "runDate");

        Topic topic = loadTopicPort.load(topicId)
                .orElseThrow(() -> new ContentException("토픽을 찾을 수 없습니다: " + topicId));

        List<ArticleScore> scores = loadArticleScoresPort.loadByTopic(
                topicId, runDate.atStartOfDay(ZoneOffset.UTC).toInstant());
        List<IssueItem> items = RankSelector.select(topic, scores);

        Long issueId = saveIssuePort.save(Issue.draft(topicId, runDate, titleOf(topic, runDate), items));

        log.info("이슈 생성 완료: topicId={}, runDate={}, 점수={}건 → 게재={}건, issueId={}",
                topicId, runDate, scores.size(), items.size(), issueId);
        return issueId;
    }

    /** MVP는 {@code "{토픽명} {발행일}"} 수준으로 단순 생성한다 — 문구 다듬기는 M3 발송 템플릿 몫. */
    private static String titleOf(Topic topic, LocalDate runDate) {
        return topic.getName() + " " + runDate;
    }
}
