package com.siftnews.content.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TopicFilterTest {

    private static Topic topic(List<String> includeKeywords, List<String> excludeKeywords,
                               List<String> sourceCategories, String langScope) {
        return Topic.create("개발", "dev", langScope, includeKeywords, excludeKeywords,
                Map.of(), sourceCategories, 24, 10, 0.0, true);
    }

    private static Topic devTopic() {
        return topic(List.of("Spring", "백엔드"), List.of("광고", "홍보"), List.of("dev", "programming"), "ko,en");
    }

    private static CandidateArticle article(String title, String body, String lang, String category) {
        return new CandidateArticle(1L, 7L, "https://ex.com/a", title, lang, body,
                Instant.parse("2026-07-26T00:00:00Z"), category, "c-1");
    }

    @Test
    void passesWhenAllRulesSatisfied() {
        assertThat(TopicFilter.matches(devTopic(), article("Spring 배치 튜닝", "본문", "ko", "DEV"))).isTrue();
    }

    @Test
    void rejectsWhenLangOutOfScope() {
        assertThat(TopicFilter.matches(devTopic(), article("Spring 배치", "본문", "ja", "DEV"))).isFalse();
    }

    @Test
    void rejectsWhenSourceCategoryNotWhitelisted() {
        assertThat(TopicFilter.matches(devTopic(), article("Spring 배치", "본문", "ko", "ECONOMY"))).isFalse();
    }

    /** 토픽은 소문자(`dev`), 기사는 source enum 이름(`DEV`)이라 대소문자를 무시해야 한다. */
    @Test
    void matchesSourceCategoryIgnoringCase() {
        assertThat(TopicFilter.matches(devTopic(), article("Spring 배치", "본문", "ko", "dev"))).isTrue();
    }

    @Test
    void rejectsWhenExcludedKeywordInTitle() {
        assertThat(TopicFilter.matches(devTopic(), article("Spring 광고", "본문", "ko", "DEV"))).isFalse();
    }

    /** 제외어는 제목뿐 아니라 본문에서도 걸러야 한다 — 제목만 보면 본문 광고를 놓친다. */
    @Test
    void rejectsWhenExcludedKeywordInBody() {
        assertThat(TopicFilter.matches(devTopic(), article("Spring 배치", "이 글은 홍보 입니다", "ko", "DEV"))).isFalse();
    }

    @Test
    void rejectsWhenNoIncludedKeywordAnywhere() {
        assertThat(TopicFilter.matches(devTopic(), article("주식 시황", "환율 이야기", "ko", "DEV"))).isFalse();
    }

    @Test
    void passesWhenIncludedKeywordOnlyInBody() {
        assertThat(TopicFilter.matches(devTopic(), article("오늘의 기록", "백엔드 아키텍처 정리", "ko", "DEV"))).isTrue();
    }

    /**
     * 빈 조건은 <b>제약 없음</b>이다 — 시드 토픽이 excludeKeywords를 빈 리스트로 두는데
     * 이걸 "전부 탈락"으로 읽으면 후보가 통째로 사라진다.
     */
    @Test
    void emptyConditionsImposeNoRestriction() {
        Topic unrestricted = topic(List.of(), List.of(), List.of(), "ko,en");

        assertThat(TopicFilter.matches(unrestricted, article("아무 제목", "아무 본문", "ko", "ECONOMY"))).isTrue();
    }

    @Test
    void rejectsWhenCategoryMissingButWhitelistDefined() {
        assertThat(TopicFilter.matches(devTopic(), article("Spring 배치", "본문", "ko", null))).isFalse();
    }

    /** 영어 키워드는 대소문자를 무시한다 — 제목 표기가 `spring`이어도 같은 기사다. */
    @Test
    void matchesKeywordIgnoringCase() {
        assertThat(TopicFilter.matches(devTopic(), article("spring boot 3.5", "본문", "en", "DEV"))).isTrue();
    }
}
