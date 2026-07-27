package com.siftnews.content.application.service;

import com.siftnews.content.application.port.out.LoadArticleScoresPort;
import com.siftnews.content.application.port.out.LoadTopicPort;
import com.siftnews.content.application.port.out.SaveIssuePort;
import com.siftnews.content.domain.ArticleScore;
import com.siftnews.content.domain.ContentException;
import com.siftnews.content.domain.Issue;
import com.siftnews.content.domain.IssueItem;
import com.siftnews.content.domain.IssueStatus;
import com.siftnews.content.domain.ScoreBreakdown;
import com.siftnews.content.domain.ScoreWeights;
import com.siftnews.content.domain.Topic;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BuildIssueServiceTest {

    private static final Long TOPIC_ID = 3L;
    private static final LocalDate RUN_DATE = LocalDate.parse("2026-07-27");
    private static final Instant COMPUTED_AT = Instant.parse("2026-07-27T06:00:00Z");

    private static Topic devTopic(int maxItems, double threshold) {
        return Topic.restore(TOPIC_ID, "개발", "dev", "ko,en", List.of("Spring"), List.of(),
                Map.of(), List.of("dev"), 24, maxItems, threshold, true);
    }

    private static ArticleScore score(long articleId, long sourceId, double value) {
        ScoreBreakdown breakdown = new ScoreBreakdown(value, 0, 0, 0, List.of(), ScoreWeights.DEFAULT);
        return new ArticleScore(articleId, sourceId, TOPIC_ID, value, breakdown, COMPUTED_AT);
    }

    private record FakeLoadTopicPort(Topic topic) implements LoadTopicPort {
        @Override
        public Optional<Topic> load(Long topicId) {
            return Optional.ofNullable(topic);
        }
    }

    private static final class FakeLoadArticleScoresPort implements LoadArticleScoresPort {
        private final List<ArticleScore> scores;
        private Instant capturedLowerBound;

        private FakeLoadArticleScoresPort(List<ArticleScore> scores) {
            this.scores = scores;
        }

        @Override
        public List<ArticleScore> loadByTopic(Long topicId, Instant computedAtFrom) {
            this.capturedLowerBound = computedAtFrom;
            return scores;
        }
    }

    private static final class FakeSaveIssuePort implements SaveIssuePort {
        private final List<Issue> saved = new ArrayList<>();

        @Override
        public Long save(Issue issue) {
            saved.add(issue);
            return 100L;
        }
    }

    @Test
    void buildsDraftIssueFromScores() {
        FakeSaveIssuePort savePort = new FakeSaveIssuePort();
        BuildIssueService service = new BuildIssueService(new FakeLoadTopicPort(devTopic(10, 0.0)),
                new FakeLoadArticleScoresPort(List.of(score(1L, 10L, 0.4), score(2L, 20L, 0.9))), savePort);

        Long issueId = service.buildIssueForTopic(TOPIC_ID, RUN_DATE);

        assertThat(issueId).isEqualTo(100L);
        Issue issue = savePort.saved.get(0);
        assertThat(issue.getStatus()).isEqualTo(IssueStatus.DRAFT);
        assertThat(issue.getRunDate()).isEqualTo(RUN_DATE);
        assertThat(issue.getItems()).extracting(IssueItem::articleId).containsExactly(2L, 1L);
    }

    @Test
    void titleCarriesTopicNameAndRunDate() {
        FakeSaveIssuePort savePort = new FakeSaveIssuePort();
        new BuildIssueService(new FakeLoadTopicPort(devTopic(10, 0.0)),
                new FakeLoadArticleScoresPort(List.of()), savePort).buildIssueForTopic(TOPIC_ID, RUN_DATE);

        assertThat(savePort.saved.get(0).getTitle()).isEqualTo("개발 2026-07-27");
    }

    /**
     * {@code article_score}는 기사·토픽 쌍으로 upsert되어 과거 점수가 계속 남는다 —
     * 하한이 없으면 몇 주 전 기사가 오늘 호에 섞인다.
     */
    @Test
    void loadsOnlyScoresComputedOnRunDate() {
        FakeLoadArticleScoresPort loadPort = new FakeLoadArticleScoresPort(List.of());
        new BuildIssueService(new FakeLoadTopicPort(devTopic(10, 0.0)), loadPort, new FakeSaveIssuePort())
                .buildIssueForTopic(TOPIC_ID, RUN_DATE);

        assertThat(loadPort.capturedLowerBound).isEqualTo(Instant.parse("2026-07-27T00:00:00Z"));
    }

    @Test
    void appliesTopicThresholdAndMaxItems() {
        FakeSaveIssuePort savePort = new FakeSaveIssuePort();
        new BuildIssueService(new FakeLoadTopicPort(devTopic(2, 0.5)),
                new FakeLoadArticleScoresPort(List.of(
                        score(1L, 10L, 0.9), score(2L, 20L, 0.8), score(3L, 30L, 0.7), score(4L, 40L, 0.1))),
                savePort).buildIssueForTopic(TOPIC_ID, RUN_DATE);

        assertThat(savePort.saved.get(0).getItems()).hasSize(2);
    }

    /** 뽑힌 게 없어도 호는 만든다 — 없으면 "아직 안 돌았다"와 구분되지 않는다. */
    @Test
    void createsIssueEvenWhenNothingSelected() {
        FakeSaveIssuePort savePort = new FakeSaveIssuePort();
        new BuildIssueService(new FakeLoadTopicPort(devTopic(10, 0.9)),
                new FakeLoadArticleScoresPort(List.of(score(1L, 10L, 0.1))), savePort)
                .buildIssueForTopic(TOPIC_ID, RUN_DATE);

        assertThat(savePort.saved).hasSize(1);
        assertThat(savePort.saved.get(0).getItems()).isEmpty();
    }

    @Test
    void rerunProducesIdenticalItems() {
        List<ArticleScore> scores = List.of(score(1L, 10L, 0.4), score(2L, 20L, 0.9));

        FakeSaveIssuePort first = new FakeSaveIssuePort();
        new BuildIssueService(new FakeLoadTopicPort(devTopic(10, 0.0)),
                new FakeLoadArticleScoresPort(scores), first).buildIssueForTopic(TOPIC_ID, RUN_DATE);
        FakeSaveIssuePort second = new FakeSaveIssuePort();
        new BuildIssueService(new FakeLoadTopicPort(devTopic(10, 0.0)),
                new FakeLoadArticleScoresPort(scores), second).buildIssueForTopic(TOPIC_ID, RUN_DATE);

        assertThat(second.saved.get(0).getItems()).isEqualTo(first.saved.get(0).getItems());
    }

    @Test
    void failsWhenTopicNotFound() {
        assertThatThrownBy(() -> new BuildIssueService(new FakeLoadTopicPort(null),
                new FakeLoadArticleScoresPort(List.of()), new FakeSaveIssuePort())
                .buildIssueForTopic(TOPIC_ID, RUN_DATE))
                .isInstanceOf(ContentException.class);
    }
}
