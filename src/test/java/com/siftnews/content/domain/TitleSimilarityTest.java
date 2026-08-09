package com.siftnews.content.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TitleSimilarityTest {

    @Test
    void identicalTitlesAreOne() {
        assertThat(TitleSimilarity.jaccard("Spring Boot 3 released", "Spring Boot 3 released"))
                .isEqualTo(1.0);
    }

    @Test
    void disjointTitlesAreZero() {
        assertThat(TitleSimilarity.jaccard("Bitcoin hits high", "Kubernetes 최신 릴리스"))
                .isEqualTo(0.0);
    }

    @Test
    void partialOverlapIsRatio() {
        // {spring,boot,3,released} vs {spring,boot,3,released,notes} = 4/5
        assertThat(TitleSimilarity.jaccard("Spring Boot 3 released", "Spring Boot 3 released notes"))
                .isCloseTo(0.8, within(1e-9));
    }

    @Test
    void blankTitleIsZero() {
        assertThat(TitleSimilarity.jaccard("", "anything")).isEqualTo(0.0);
        assertThat(TitleSimilarity.jaccard(null, "anything")).isEqualTo(0.0);
    }

    @Test
    void punctuationAndCaseIgnored() {
        assertThat(TitleSimilarity.jaccard("Spring-Boot, RELEASED!", "spring boot released"))
                .isEqualTo(1.0);
    }
}
