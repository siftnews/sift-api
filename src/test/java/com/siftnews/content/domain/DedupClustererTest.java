package com.siftnews.content.domain;

import com.siftnews.common.UriNormalizer;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DedupClustererTest {

    private static final double THRESHOLD = 0.7;

    private static CandidateArticle article(long id, String normalizedUrl, String title, Instant publishedAt) {
        return new CandidateArticle(id, id, normalizedUrl, title, "en", "body", publishedAt, "DEV", null);
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

    /**
     * 정규화 → dedup 연결 지점의 회귀 (2026-08-01) — 병합 기준 ①이 {@code normalizedUrl} 완전
     * 일치라, 정규화가 쿼리를 통째로 버리던 시절에는 <b>제목이 전혀 다른 기사들이 한 클러스터로
     * 병합</b>됐다. 적재 실패({@code UNIQUE}) 뒤에 가려져 있던 두 번째 피해다.
     */
    @Test
    void articlesDistinguishedOnlyByQueryStayInSeparateClusters() {
        String first = UriNormalizer.normalize("https://www.aitimes.com/news/articleView.html?idxno=213427");
        String second = UriNormalizer.normalize("https://www.aitimes.com/news/articleView.html?idxno=213385");

        List<ArticleCluster> clusters = DedupClusterer.cluster(List.of(
                article(1L, first, "완전히 다른 제목 하나", Instant.parse("2026-07-20T00:00:00Z")),
                article(2L, second, "전혀 무관한 제목 둘", Instant.parse("2026-07-21T00:00:00Z"))
        ), THRESHOLD);

        assertThat(clusters).hasSize(2);
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
        assertThat(clusters.get(0).clusterId()).isEqualTo("c-1");
    }

    @Test
    void emptyInputYieldsNoClusters() {
        assertThat(DedupClusterer.cluster(List.of(), THRESHOLD)).isEmpty();
    }

    @Test
    void clusterIdStaysStableWhenNewerArticleJoins() {
        List<ArticleCluster> firstRun = DedupClusterer.cluster(List.of(
                article(1L, "https://ex.com/a", "같은 사건 보도", Instant.parse("2026-07-20T00:00:00Z")),
                article(2L, "https://ex.com/a", "같은 사건 보도", Instant.parse("2026-07-21T00:00:00Z"))
        ), THRESHOLD);
        String firstClusterId = firstRun.get(0).clusterId();

        // 더 최신 publishedAt + 더 큰 articleId를 가진 기사(3L)가 재실행 시 합류한다.
        List<ArticleCluster> secondRun = DedupClusterer.cluster(List.of(
                article(1L, "https://ex.com/a", "같은 사건 보도", Instant.parse("2026-07-20T00:00:00Z")),
                article(2L, "https://ex.com/a", "같은 사건 보도", Instant.parse("2026-07-21T00:00:00Z")),
                article(3L, "https://ex.com/a", "같은 사건 보도", Instant.parse("2026-07-22T00:00:00Z"))
        ), THRESHOLD);

        assertThat(secondRun).hasSize(1);
        // clusterId는 재실행에 안정적으로 유지되지만, 대표는 최신 기사로 바뀐다 — id 발급 기준과 대표 선정을 분리.
        assertThat(secondRun.get(0).clusterId()).isEqualTo(firstClusterId);
        assertThat(secondRun.get(0).representativeId()).isEqualTo(3L);
    }

    @Test
    void clusterIdIsDerivedFromMinMemberId() {
        // min 멤버(3L)와 대표(최신 발행 9L)가 서로 다른 기사가 되도록 배치 —
        // 구 코드("c-" + representative.articleId())였다면 "c-9"가 나와 이 테스트가 실패한다.
        List<ArticleCluster> clusters = DedupClusterer.cluster(List.of(
                article(5L, "https://ex.com/a", "같은 사건 보도", Instant.parse("2026-07-20T00:00:00Z")),
                article(3L, "https://ex.com/a", "같은 사건 보도", Instant.parse("2026-07-19T00:00:00Z")),
                article(9L, "https://ex.com/a", "같은 사건 보도", Instant.parse("2026-07-23T00:00:00Z"))
        ), THRESHOLD);

        assertThat(clusters).hasSize(1);
        assertThat(clusters.get(0).clusterId()).isEqualTo("c-3");
        assertThat(clusters.get(0).representativeId()).isEqualTo(9L);
    }
}
