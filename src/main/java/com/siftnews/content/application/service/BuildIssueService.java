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

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * 선별 3/3 — 점수 로드 → threshold 컷·랭킹·소스 다양성 → 상위 {@code maxItems}건으로 호 생성 (SELECTION §2.5).
 * <p>
 * 점수는 다시 계산하지 않는다 — {@code scoreTopic}(선별 2/3)이 저장한 {@code article_score}를 읽는다.
 * <p>
 * 로드 하한은 <b>호출자가 넘긴 선별 윈도우의 {@code from}</b>이다. {@code article_score}는
 * 기사·토픽 쌍으로 upsert되어 과거 점수가 계속 남으므로, 하한이 없으면 몇 주 전 기사가 오늘 호에 섞인다.
 * <p>
 * 하한을 {@code runDate}에서 유도하지 않는다 — {@code LocalDate}에는 존 정보가 없어 {@code Instant}로
 * 바꾸려면 <b>기준 존을 골라야 하고</b>, 그 존이 {@code runDate}를 계산한 트리거의 존과 어긋나면 조회가 통째로 빈다.
 * (KST 날짜의 UTC 자정은 KST 09:00이라, 06:00 발행에서는 그날 계산된 점수가 전부 하한 미만이었다 — #35)
 * 윈도우 {@code from}을 그대로 쓰면 존 변환이 없고, 스코어링이 계산한 범위와 호가 읽는 범위도 일치한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BuildIssueService implements BuildIssueUseCase {

    private final LoadTopicPort loadTopicPort;
    private final LoadArticleScoresPort loadArticleScoresPort;
    private final SaveIssuePort saveIssuePort;

    @Override
    public Long buildIssueForTopic(Long topicId, LocalDate runDate, Instant scoredFrom) {
        Objects.requireNonNull(topicId, "topicId");
        Objects.requireNonNull(runDate, "runDate");
        Objects.requireNonNull(scoredFrom, "scoredFrom");

        Topic topic = loadTopicPort.load(topicId)
                .orElseThrow(() -> new ContentException("토픽을 찾을 수 없습니다: " + topicId));

        List<ArticleScore> scores = loadArticleScoresPort.loadByTopic(topicId, scoredFrom);
        List<IssueItem> items = RankSelector.select(topic, scores);

        Long issueId = saveIssuePort.save(Issue.draft(topicId, runDate, titleOf(topic, runDate), items));

        // 하한을 함께 남긴다 — 조회가 0건일 때 "점수가 없는 것"과 "하한이 잘못된 것"을 로그만으로 가르기 위해서다(#35).
        log.info("이슈 생성 완료: topicId={}, runDate={}, scoredFrom={}, 점수={}건 → 게재={}건, issueId={}",
                topicId, runDate, scoredFrom, scores.size(), items.size(), issueId);
        return issueId;
    }

    /** MVP는 {@code "{토픽명} {발행일}"} 수준으로 단순 생성한다 — 문구 다듬기는 M3 발송 템플릿 몫. */
    private static String titleOf(Topic topic, LocalDate runDate) {
        return topic.getName() + " " + runDate;
    }
}
