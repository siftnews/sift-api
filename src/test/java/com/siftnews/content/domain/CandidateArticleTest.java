package com.siftnews.content.domain;

import com.siftnews.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CandidateArticleTest {

    @Test
    void articleIdIsRequired() {
        assertThatThrownBy(() -> new CandidateArticle(null, "https://ex.com/a", "제목", "en", "body", Instant.now()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void constructsWithAllFieldsPresent() {
        CandidateArticle article = new CandidateArticle(1L, "https://ex.com/a", "제목", "en", "body", Instant.now());

        assertThat(article.articleId()).isEqualTo(1L);
    }

    @Test
    void publishedAtMayBeNullForFeedsWithoutTimestamp() {
        // 발행시각 없는 피드를 위한 의도된 설계 — 대표 선정(DedupClusterer)이 Instant.MIN으로 방어한다.
        CandidateArticle article = new CandidateArticle(1L, "https://ex.com/a", "제목", "en", "body", null);

        assertThat(article.publishedAt()).isNull();
    }
}
