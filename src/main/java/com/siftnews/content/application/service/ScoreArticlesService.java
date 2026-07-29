package com.siftnews.content.application.service;

import com.siftnews.content.application.port.in.ScoreArticlesSummary;
import com.siftnews.content.application.port.in.ScoreArticlesUseCase;
import com.siftnews.content.application.port.out.LoadCandidateArticlesPort;
import com.siftnews.content.application.port.out.LoadTopicPort;
import com.siftnews.content.application.port.out.SaveArticleScorePort;
import com.siftnews.content.domain.ArticleScore;
import com.siftnews.content.domain.ArticleScorer;
import com.siftnews.content.domain.CandidateArticle;
import com.siftnews.content.domain.ClusterWindow;
import com.siftnews.content.domain.ContentException;
import com.siftnews.content.domain.RepresentativeRule;
import com.siftnews.content.domain.ScoreWeights;
import com.siftnews.content.domain.Topic;
import com.siftnews.content.domain.TopicFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 선별 2/3 — 후보 로드 → 토픽 필터 → 클러스터당 대표 한 건 → 가중합 스코어 → 저장 (SELECTION §2.3~2.4).
 * <p>
 * 순서에 이유가 있다.
 * <ol>
 *   <li><b>화제성 집계는 필터 전에</b> — 클러스터 크기는 "몇 개 매체가 다뤘나"라 토픽과 무관하다.
 *       필터 뒤 집합으로 세면 토픽마다 같은 사건의 화제성이 달라진다.</li>
 *   <li><b>대표 선정은 필터 뒤에</b> — 먼저 대표를 뽑으면 그 대표가 토픽 필터에 걸렸을 때
 *       통과할 수 있었던 같은 클러스터의 다른 기사까지 통째로 잃는다.</li>
 * </ol>
 * 재실행 멱등성은 {@code SaveArticleScorePort}가 {@code (articleId, topicId)} 기준으로
 * 덮어써서 보장한다 — 같은 윈도우를 다시 돌려도 행이 늘지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreArticlesService implements ScoreArticlesUseCase {

    private final LoadTopicPort loadTopicPort;
    private final LoadCandidateArticlesPort loadCandidateArticlesPort;
    private final SaveArticleScorePort saveArticleScorePort;
    private final Clock clock;

    @Override
    public ScoreArticlesSummary scoreTopic(Long topicId, Instant from, Instant to) {
        Objects.requireNonNull(topicId, "topicId");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (!from.isBefore(to)) {
            throw new ContentException("from은 to보다 앞서야 한다: from=" + from + ", to=" + to);
        }

        Topic topic = loadTopicPort.load(topicId)
                .orElseThrow(() -> new ContentException("토픽을 찾을 수 없습니다: " + topicId));

        List<CandidateArticle> loaded = loadCandidateArticlesPort.loadCandidates(from, to);
        ClusterWindow clusters = ClusterWindow.of(loaded);

        List<CandidateArticle> filtered = loaded.stream()
                .filter(article -> TopicFilter.matches(topic, article))
                .toList();
        List<CandidateArticle> representatives = RepresentativeRule.reduceToRepresentatives(filtered);

        Instant now = clock.instant();
        List<ArticleScore> scores = representatives.stream()
                .map(article -> ArticleScorer.score(topic, article,
                        clusters.sizeOf(article), clusters.maxSize(), now, ScoreWeights.DEFAULT))
                .toList();

        // 후보 0건은 정상 상황(첫 기동·좁은 윈도우·필터가 전부 거름) — 빈 저장 호출은 생략한다.
        if (!scores.isEmpty()) {
            saveArticleScorePort.saveAll(scores);
        }

        log.info("스코어링 완료: topicId={}, loaded={}, filtered={}, scored={}",
                topicId, loaded.size(), filtered.size(), scores.size());
        return new ScoreArticlesSummary(loaded.size(), filtered.size(), scores.size());
    }
}
