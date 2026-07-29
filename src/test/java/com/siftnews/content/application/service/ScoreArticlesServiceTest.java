package com.siftnews.content.application.service;

import com.siftnews.content.application.port.in.ScoreArticlesSummary;
import com.siftnews.content.application.port.out.LoadCandidateArticlesPort;
import com.siftnews.content.application.port.out.LoadTopicPort;
import com.siftnews.content.application.port.out.SaveArticleScorePort;
import com.siftnews.content.domain.ArticleScore;
import com.siftnews.content.domain.CandidateArticle;
import com.siftnews.content.domain.ContentException;
import com.siftnews.content.domain.Topic;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScoreArticlesServiceTest {

    private static final Long TOPIC_ID = 3L;
    private static final Instant NOW = Instant.parse("2026-07-26T12:00:00Z");
    private static final Instant FROM = Instant.parse("2026-07-25T12:00:00Z");
    private static final Instant TO = Instant.parse("2026-07-26T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private static Topic devTopic() {
        return Topic.restore(TOPIC_ID, "개발", "dev", "ko,en",
                List.of("Spring"), List.of("광고"), Map.of(), List.of("dev"), 24, 10, 0.0, true);
    }

    private static CandidateArticle article(long id, String title, String clusterId) {
        return new CandidateArticle(id, id, "https://ex.com/" + id, title, "ko", "본문",
                NOW.minusSeconds(id * 60), "DEV", clusterId);
    }

    private static final class FakeSaveArticleScorePort implements SaveArticleScorePort {
        private final List<ArticleScore> saved = new ArrayList<>();

        @Override
        public void saveAll(List<ArticleScore> scores) {
            saved.addAll(scores);
        }
    }

    private record FakeLoadTopicPort(Topic topic) implements LoadTopicPort {
        @Override
        public Optional<Topic> load(Long topicId) {
            return Optional.ofNullable(topic);
        }

        @Override
        public List<Topic> loadActive() {
            return topic == null ? List.of() : List.of(topic);
        }
    }

    private record FakeLoadCandidateArticlesPort(List<CandidateArticle> candidates)
            implements LoadCandidateArticlesPort {
        @Override
        public List<CandidateArticle> loadCandidates(Instant from, Instant to) {
            return candidates;
        }
    }

    private static ScoreArticlesService service(Topic topic, List<CandidateArticle> candidates,
                                                FakeSaveArticleScorePort savePort) {
        return new ScoreArticlesService(new FakeLoadTopicPort(topic),
                new FakeLoadCandidateArticlesPort(candidates), savePort, CLOCK);
    }

    @Test
    void scoresOnlyArticlesPassingTopicFilter() {
        FakeSaveArticleScorePort savePort = new FakeSaveArticleScorePort();
        List<CandidateArticle> candidates = List.of(
                article(1L, "Spring 배치 튜닝", null),
                article(2L, "환율 급등", null));

        ScoreArticlesSummary summary = service(devTopic(), candidates, savePort).scoreTopic(TOPIC_ID, FROM, TO);

        assertThat(summary.loaded()).isEqualTo(2);
        assertThat(summary.filtered()).isEqualTo(1);
        assertThat(savePort.saved).extracting(ArticleScore::articleId).containsExactly(1L);
    }

    /** 같은 클러스터가 여러 건 통과해도 뉴스레터에는 한 건만 — 대표만 점수를 받는다. */
    @Test
    void reducesEachClusterToItsRepresentative() {
        FakeSaveArticleScorePort savePort = new FakeSaveArticleScorePort();
        List<CandidateArticle> candidates = List.of(
                article(1L, "Spring 배치 튜닝", "c-1"),
                article(2L, "Spring 배치 튜닝 후기", "c-1"),
                article(5L, "Spring 시큐리티", "c-5"));

        ScoreArticlesSummary summary = service(devTopic(), candidates, savePort).scoreTopic(TOPIC_ID, FROM, TO);

        assertThat(summary.filtered()).isEqualTo(3);
        assertThat(summary.scored()).isEqualTo(2);
        // 대표 = 최신 발행분 → id가 작을수록 최근이도록 만들었으므로 1L·5L
        assertThat(savePort.saved).extracting(ArticleScore::articleId).containsExactly(1L, 5L);
    }

    /**
     * 화제성은 토픽과 무관한 값이라 <b>필터 전</b> 후보 전체로 센다 — 필터 뒤로 세면
     * 같은 사건의 화제성이 토픽마다 달라진다.
     */
    @Test
    void trendScoreCountsClusterSizeBeforeTopicFilter() {
        FakeSaveArticleScorePort savePort = new FakeSaveArticleScorePort();
        List<CandidateArticle> candidates = List.of(
                article(1L, "Spring 배치", "c-1"),
                article(2L, "환율 급등", "c-1"),          // 필터 탈락이지만 같은 클러스터
                article(3L, "환율 하락", "c-1"),          // 필터 탈락이지만 같은 클러스터
                article(5L, "Spring 시큐리티", "c-5"));

        service(devTopic(), candidates, savePort).scoreTopic(TOPIC_ID, FROM, TO);

        ArticleScore clustered = savePort.saved.stream()
                .filter(score -> score.articleId().equals(1L)).findFirst().orElseThrow();
        ArticleScore singleton = savePort.saved.stream()
                .filter(score -> score.articleId().equals(5L)).findFirst().orElseThrow();

        // c-1은 3건, c-5는 1건 → 최대 3 기준으로 (3-1)/(3-1)=1.0 과 (1-1)/(3-1)=0.0
        assertThat(clustered.breakdown().trendScore()).isEqualTo(1.0);
        assertThat(singleton.breakdown().trendScore()).isZero();
    }

    /** 같은 입력을 다시 돌리면 같은 결과 — 저장 포트가 덮어쓰므로 행은 늘지 않아야 한다. */
    @Test
    void rerunProducesIdenticalScores() {
        List<CandidateArticle> candidates = List.of(article(1L, "Spring 배치", "c-1"));

        FakeSaveArticleScorePort first = new FakeSaveArticleScorePort();
        service(devTopic(), candidates, first).scoreTopic(TOPIC_ID, FROM, TO);
        FakeSaveArticleScorePort second = new FakeSaveArticleScorePort();
        service(devTopic(), candidates, second).scoreTopic(TOPIC_ID, FROM, TO);

        assertThat(second.saved).hasSize(1);
        assertThat(second.saved.get(0).score()).isEqualTo(first.saved.get(0).score());
        assertThat(second.saved.get(0).breakdown()).isEqualTo(first.saved.get(0).breakdown());
    }

    @Test
    void failsWhenTopicNotFound() {
        assertThatThrownBy(() -> service(null, List.of(), new FakeSaveArticleScorePort())
                .scoreTopic(TOPIC_ID, FROM, TO))
                .isInstanceOf(ContentException.class);
    }

    @Test
    void rejectsInvalidWindow() {
        assertThatThrownBy(() -> service(devTopic(), List.of(), new FakeSaveArticleScorePort())
                .scoreTopic(TOPIC_ID, TO, FROM))
                .isInstanceOf(ContentException.class);
    }

    /** 후보 0건은 정상 상황이다 — 빈 저장 호출로 어댑터를 괴롭히지 않는다. */
    @Test
    void skipsSaveWhenNothingScored() {
        FakeSaveArticleScorePort savePort = new FakeSaveArticleScorePort();

        ScoreArticlesSummary summary = service(devTopic(), List.of(), savePort).scoreTopic(TOPIC_ID, FROM, TO);

        assertThat(summary.scored()).isZero();
        assertThat(savePort.saved).isEmpty();
    }
}
