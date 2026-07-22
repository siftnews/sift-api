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

class NormalizeDedupServiceTest {

    private static final String LONG_BODY = "x".repeat(250);

    private static CandidateArticle article(long id, String url, String title, String lang, String body) {
        return new CandidateArticle(id, url, title, lang, body, Instant.parse("2026-07-2" + (id % 10) + "T00:00:00Z"));
    }

    @Test
    void dropsCutArticlesAndAssignsClustersToSurvivors() {
        List<CandidateArticle> candidates = List.of(
                article(1L, "https://ex.com/a", "Spring Boot 3 released", "en", LONG_BODY),
                article(2L, "https://ex.com/a", "무관 제목이지만 같은 URL", "en", LONG_BODY),   // a1과 같은 URL → 병합
                article(3L, "https://b.com/y", "Bitcoin price surges", "en", LONG_BODY),      // 독립 클러스터
                article(4L, "https://c.com/z", "짧은 본문", "en", "너무 짧음"),                  // 본문 짧아 drop
                article(5L, "https://d.com/w", "unsupported language", "fr", LONG_BODY));      // 언어 drop

        FakeLoadCandidateArticlesPort loadPort = new FakeLoadCandidateArticlesPort(candidates);
        FakeUpdateArticleClusterPort updatePort = new FakeUpdateArticleClusterPort();
        NormalizeDedupService service = new NormalizeDedupService(loadPort, updatePort);

        NormalizeDedupSummary summary = service.normalizeAndDedup();

        assertThat(summary.loaded()).isEqualTo(5);
        assertThat(summary.dropped()).isEqualTo(2);
        assertThat(summary.clusters()).isEqualTo(2);

        Map<Long, String> assignments = updatePort.assignments;
        assertThat(assignments).containsOnlyKeys(1L, 2L, 3L);          // drop된 4·5는 미할당
        assertThat(assignments.get(1L)).isEqualTo(assignments.get(2L)); // 같은 URL → 같은 클러스터
        assertThat(assignments.get(3L)).isNotEqualTo(assignments.get(1L));
    }

    private static final class FakeLoadCandidateArticlesPort implements LoadCandidateArticlesPort {
        private final List<CandidateArticle> candidates;

        private FakeLoadCandidateArticlesPort(List<CandidateArticle> candidates) {
            this.candidates = candidates;
        }

        @Override
        public List<CandidateArticle> loadCandidates() {
            return candidates;
        }
    }

    private static final class FakeUpdateArticleClusterPort implements UpdateArticleClusterPort {
        private final Map<Long, String> assignments = new HashMap<>();

        @Override
        public void updateCluster(Long articleId, String dedupClusterId) {
            assignments.put(articleId, dedupClusterId);
        }
    }
}
