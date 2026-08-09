package com.siftnews.content.domain;

import com.siftnews.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CandidateArticleTest {

    private static CandidateArticle article(Long articleId, Long sourceId, Instant publishedAt) {
        return new CandidateArticle(articleId, sourceId, "https://ex.com/a", "제목", "en", "body",
                publishedAt, "DEV", "c-1");
    }

    @Test
    void articleIdIsRequired() {
        assertThatThrownBy(() -> article(null, 7L, Instant.now()))
                .isInstanceOf(BusinessException.class);
    }

    /** sourceId는 sourceScore·필터 진단의 입력이라, 없으면 점수가 조용히 틀어진다. */
    @Test
    void sourceIdIsRequired() {
        assertThatThrownBy(() -> article(1L, null, Instant.now()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void constructsWithAllFieldsPresent() {
        CandidateArticle article = article(1L, 7L, Instant.now());

        assertThat(article.articleId()).isEqualTo(1L);
        assertThat(article.sourceId()).isEqualTo(7L);
        assertThat(article.category()).isEqualTo("DEV");
        assertThat(article.dedupClusterId()).isEqualTo("c-1");
    }

    @Test
    void publishedAtMayBeNullForFeedsWithoutTimestamp() {
        // 발행시각 없는 피드를 위한 의도된 설계 — 대표 선정(DedupClusterer)이 Instant.MIN으로 방어한다.
        CandidateArticle article = article(1L, 7L, null);

        assertThat(article.publishedAt()).isNull();
    }

    /**
     * 컷 탈락·미클러스터 기사는 {@code dedupClusterId}가 null이다(D-031) — 화제성 점수가
     * 이 값을 세므로, null을 막으면 아직 클러스터링 안 된 기사를 후보로 실을 수 없게 된다.
     */
    @Test
    void dedupClusterIdMayBeNullBeforeClustering() {
        CandidateArticle article = new CandidateArticle(1L, 7L, "https://ex.com/a", "제목", "en", "body",
                Instant.now(), "DEV", null);

        assertThat(article.dedupClusterId()).isNull();
    }
}
