package com.siftnews.content.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DedupClustererTest {

    private static final double THRESHOLD = 0.7;

    private static CandidateArticle article(long id, String normalizedUrl, String title, Instant publishedAt) {
        return new CandidateArticle(id, normalizedUrl, title, "en", "body", publishedAt);
    }

    @Test
    void sameNormalizedUrlFormsOneCluster() {
        List<ArticleCluster> clusters = DedupClusterer.cluster(List.of(
                article(1L, "https://ex.com/a", "완전히 다른 제목 하나", Instant.parse("2026-07-20T00:00:00Z")),
                article(2L, "https://ex.com/a", "전혀 무관한 제목 둘", Instant.parse("2026-07-21T00:00:00Z"))
        ), THRESHOLD);

        assertThat(clusters).hasSize(1);
        assertThat(clusters.get(0).memberIds()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void similarTitlesFormOneClusterEvenWithDifferentUrls() {
        List<ArticleCluster> clusters = DedupClusterer.cluster(List.of(
                article(1L, "https://a.com/x", "Spring Boot 3 released today", Instant.parse("2026-07-20T00:00:00Z")),
                article(2L, "https://b.com/y", "Spring Boot 3 released today news", Instant.parse("2026-07-21T00:00:00Z"))
        ), THRESHOLD);

        assertThat(clusters).hasSize(1);
    }

    @Test
    void unrelatedArticlesStaySeparate() {
        List<ArticleCluster> clusters = DedupClusterer.cluster(List.of(
                article(1L, "https://a.com/x", "Spring Boot 릴리스 소식", Instant.parse("2026-07-20T00:00:00Z")),
                article(2L, "https://b.com/y", "Bitcoin price surges again", Instant.parse("2026-07-21T00:00:00Z"))
        ), THRESHOLD);

        assertThat(clusters).hasSize(2);
    }

    @Test
    void representativeIsLatestPublished() {
        List<ArticleCluster> clusters = DedupClusterer.cluster(List.of(
                article(1L, "https://ex.com/a", "같은 사건 보도", Instant.parse("2026-07-20T00:00:00Z")),
                article(2L, "https://ex.com/a", "같은 사건 보도", Instant.parse("2026-07-22T00:00:00Z"))
        ), THRESHOLD);

        assertThat(clusters).hasSize(1);
        assertThat(clusters.get(0).representativeId()).isEqualTo(2L);
        assertThat(clusters.get(0).clusterId()).isEqualTo("c-2");
    }

    @Test
    void emptyInputYieldsNoClusters() {
        assertThat(DedupClusterer.cluster(List.of(), THRESHOLD)).isEmpty();
    }
}
