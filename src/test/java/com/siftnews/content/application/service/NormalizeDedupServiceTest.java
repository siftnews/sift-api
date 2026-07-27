package com.siftnews.content.application.service;

import com.siftnews.content.application.port.in.NormalizeDedupSummary;
import com.siftnews.content.application.port.out.LoadCandidateArticlesPort;
import com.siftnews.content.application.port.out.UpdateArticleClusterPort;
import com.siftnews.content.domain.CandidateArticle;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NormalizeDedupServiceTest {

    private static final String LONG_BODY = "x".repeat(250);
    private static final Instant FROM = Instant.parse("2026-07-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-07-26T00:00:00Z");

    private static CandidateArticle article(long id, String url, String title, String lang, String body) {
        return new CandidateArticle(id, id, url, title, lang, body,
                Instant.parse("2026-07-2" + (id % 10) + "T00:00:00Z"), "DEV", null);
    }

    @Test
    void dropsCutArticlesAndAssignsClustersToSurvivors() {
        List<CandidateArticle> candidates = List.of(
                article(1L, "https://ex.com/a", "Spring Boot 3 released", "en", LONG_BODY),
                article(2L, "https://ex.com/a", "무관 제목이지만 같은 URL", "en", LONG_BODY),   // sameUrl 방어 가드 검증(운영은 UNIQUE(normalized_url)로 이 경우 없음)
                article(3L, "https://b.com/y", "Bitcoin price surges", "en", LONG_BODY),      // 독립 클러스터
                article(4L, "https://c.com/z", "짧은 본문", "en", "너무 짧음"),                  // 본문 짧아 drop
                article(5L, "https://d.com/w", "unsupported language", "fr", LONG_BODY));      // 언어 drop

        FakeLoadCandidateArticlesPort loadPort = new FakeLoadCandidateArticlesPort(candidates);
        FakeUpdateArticleClusterPort updatePort = new FakeUpdateArticleClusterPort();
        NormalizeDedupService service = new NormalizeDedupService(loadPort, updatePort);

        NormalizeDedupSummary summary = service.normalizeAndDedup(FROM, TO);

        assertThat(loadPort.lastFrom).isEqualTo(FROM);
        assertThat(loadPort.lastTo).isEqualTo(TO);

        assertThat(summary.loaded()).isEqualTo(5);
        assertThat(summary.dropped()).isEqualTo(2);
        assertThat(summary.clusters()).isEqualTo(2);

        Map<Long, String> assignments = updatePort.lastUpdate;
        assertThat(assignments).containsOnlyKeys(1L, 2L, 3L, 4L, 5L);   // 탈락 4·5도 null로 맵에 포함(상태 교체)
        assertThat(assignments.get(1L)).isEqualTo(assignments.get(2L)); // 같은 URL → 같은 클러스터
        assertThat(assignments.get(3L)).isNotEqualTo(assignments.get(1L));
        assertThat(assignments.get(4L)).isNull();
        assertThat(assignments.get(5L)).isNull();
    }

    @Test
    void clearsClusterIdWhenArticleFallsOutOfCutOnRerun() {
        FakeLoadCandidateArticlesPort loadPort = new FakeLoadCandidateArticlesPort(
                List.of(article(1L, "https://ex.com/a", "정상 기사 제목", "en", LONG_BODY)));
        FakeUpdateArticleClusterPort updatePort = new FakeUpdateArticleClusterPort();
        NormalizeDedupService service = new NormalizeDedupService(loadPort, updatePort);

        NormalizeDedupSummary firstRun = service.normalizeAndDedup(FROM, TO);
        assertThat(firstRun.dropped()).isZero();
        assertThat(updatePort.lastUpdate.get(1L)).isNotNull();

        // 2회차: 같은 articleId(1L)가 본문이 짧아져 정규화 컷에 탈락한 상태로 로드된다.
        loadPort.candidates = List.of(article(1L, "https://ex.com/a", "정상 기사 제목", "en", "너무 짧음"));
        NormalizeDedupSummary secondRun = service.normalizeAndDedup(FROM, TO);

        assertThat(secondRun.dropped()).isEqualTo(1);
        assertThat(updatePort.lastUpdate).containsKey(1L);
        assertThat(updatePort.lastUpdate.get(1L)).isNull();
    }

    @Test
    void newMemberJoinsButExistingMembersKeepClusterId() {
        FakeLoadCandidateArticlesPort loadPort = new FakeLoadCandidateArticlesPort(List.of(
                article(1L, "https://ex.com/a", "같은 사건 보도", "en", LONG_BODY),
                article(2L, "https://ex.com/a", "같은 사건 보도", "en", LONG_BODY)));
        FakeUpdateArticleClusterPort updatePort = new FakeUpdateArticleClusterPort();
        NormalizeDedupService service = new NormalizeDedupService(loadPort, updatePort);

        service.normalizeAndDedup(FROM, TO);
        String firstClusterId = updatePort.lastUpdate.get(1L);
        assertThat(firstClusterId).isNotNull();

        // 2회차: 더 최신·더 큰 id의 기사(3L)가 같은 클러스터에 합류한다.
        loadPort.candidates = List.of(
                article(1L, "https://ex.com/a", "같은 사건 보도", "en", LONG_BODY),
                article(2L, "https://ex.com/a", "같은 사건 보도", "en", LONG_BODY),
                article(3L, "https://ex.com/a", "같은 사건 보도", "en", LONG_BODY));
        service.normalizeAndDedup(FROM, TO);

        assertThat(updatePort.lastUpdate.get(1L)).isEqualTo(firstClusterId);
        assertThat(updatePort.lastUpdate.get(2L)).isEqualTo(firstClusterId);
        assertThat(updatePort.lastUpdate.get(3L)).isEqualTo(firstClusterId);
    }

    @Test
    void emptyCandidatesSkipsUpdatePortAndReturnsZeroSummary() {
        FakeLoadCandidateArticlesPort loadPort = new FakeLoadCandidateArticlesPort(List.of());
        FakeUpdateArticleClusterPort updatePort = new FakeUpdateArticleClusterPort();
        NormalizeDedupService service = new NormalizeDedupService(loadPort, updatePort);

        NormalizeDedupSummary summary = service.normalizeAndDedup(FROM, TO);

        assertThat(summary).isEqualTo(new NormalizeDedupSummary(0, 0, 0));
        assertThat(updatePort.called).isFalse();
    }

    @Test
    void rejectsWhenFromIsNotBeforeTo() {
        FakeLoadCandidateArticlesPort loadPort = new FakeLoadCandidateArticlesPort(List.of());
        FakeUpdateArticleClusterPort updatePort = new FakeUpdateArticleClusterPort();
        NormalizeDedupService service = new NormalizeDedupService(loadPort, updatePort);

        assertThatThrownBy(() -> service.normalizeAndDedup(TO, FROM))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.normalizeAndDedup(FROM, FROM))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(updatePort.called).isFalse();
    }

    @Test
    void idempotentOnRepeatedRunsWithSameInput() {
        List<CandidateArticle> candidates = List.of(
                article(1L, "https://ex.com/a", "Spring Boot 3 released", "en", LONG_BODY),
                article(3L, "https://b.com/y", "Bitcoin price surges", "en", LONG_BODY),
                article(4L, "https://c.com/z", "짧은 본문", "en", "너무 짧음"));
        FakeLoadCandidateArticlesPort loadPort = new FakeLoadCandidateArticlesPort(candidates);
        FakeUpdateArticleClusterPort updatePort = new FakeUpdateArticleClusterPort();
        NormalizeDedupService service = new NormalizeDedupService(loadPort, updatePort);

        NormalizeDedupSummary firstRun = service.normalizeAndDedup(FROM, TO);
        Map<Long, String> firstUpdate = new HashMap<>(updatePort.lastUpdate);

        NormalizeDedupSummary secondRun = service.normalizeAndDedup(FROM, TO);

        assertThat(secondRun).isEqualTo(firstRun);
        assertThat(updatePort.lastUpdate).isEqualTo(firstUpdate);
    }

    private static final class FakeLoadCandidateArticlesPort implements LoadCandidateArticlesPort {
        private List<CandidateArticle> candidates;
        private Instant lastFrom;
        private Instant lastTo;

        private FakeLoadCandidateArticlesPort(List<CandidateArticle> candidates) {
            this.candidates = candidates;
        }

        @Override
        public List<CandidateArticle> loadCandidates(Instant from, Instant to) {
            this.lastFrom = from;
            this.lastTo = to;
            return candidates;
        }
    }

    private static final class FakeUpdateArticleClusterPort implements UpdateArticleClusterPort {
        private final Map<Long, String> lastUpdate = new HashMap<>();
        private boolean called;

        @Override
        public void updateClusters(Map<Long, String> clusterIdsByArticleId) {
            called = true;
            lastUpdate.clear();
            lastUpdate.putAll(clusterIdsByArticleId);
        }
    }
}
