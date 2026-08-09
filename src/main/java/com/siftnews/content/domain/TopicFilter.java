package com.siftnews.content.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Filter 단계 (SELECTION §2.3) — 토픽별로 후보군을 추린다.
 * <p>
 * Normalize 컷({@link ArticleNormalizer})이 토픽 무관 전역 컷인 것과 달리, 이 단계는
 * <b>토픽마다</b> 다른 결과를 낸다. 통과 여부만 판정하고 순위는 매기지 않는다 —
 * 얼마나 관련 있는지는 {@link ArticleScorer}의 몫이다.
 * <p>
 * 규칙은 <b>싼 것부터</b> 본다(언어 → 카테고리 → 제외어 → 포함어). 앞의 둘은 필드 하나
 * 비교로 끝나지만 키워드 규칙은 본문 전체를 훑기 때문이다.
 * <p>
 * 비어 있는 조건은 <b>제약 없음</b>으로 읽는다 — 토픽 시드가 {@code excludeKeywords}를
 * 빈 리스트로 두는 것(TopicSeedData)은 "아무것도 거르지 말라"는 뜻이지 "전부 거르라"가
 * 아니다. 반대로 {@code includeKeywords}가 정의돼 있으면 하나 이상 맞아야 한다.
 */
public final class TopicFilter {

    private TopicFilter() {
    }

    public static boolean matches(Topic topic, CandidateArticle article) {
        return matchesLangScope(topic, article)
                && matchesSourceCategory(topic, article)
                && hasNoExcludedKeyword(topic, article)
                && hasRequiredKeyword(topic, article);
    }

    /** {@code langScope}는 {@code "ko,en"} 형태의 쉼표 목록이다 (TopicSeedData). */
    private static boolean matchesLangScope(Topic topic, CandidateArticle article) {
        String langScope = topic.getLangScope();
        if (langScope == null || langScope.isBlank()) {
            return true;
        }
        String lang = article.lang();
        if (lang == null) {
            return false;
        }
        return Arrays.stream(langScope.split(","))
                .map(scope -> scope.strip().toLowerCase(Locale.ROOT))
                .anyMatch(scope -> scope.equals(lang.strip().toLowerCase(Locale.ROOT)));
    }

    /**
     * 토픽의 {@code sourceCategories}는 소문자 문자열({@code "dev"}·{@code "programming"})이고
     * 기사 쪽은 source 모듈 enum의 이름({@code "DEV"})이라, 대소문자를 무시하고 맞춘다.
     */
    private static boolean matchesSourceCategory(Topic topic, CandidateArticle article) {
        List<String> allowed = topic.getSourceCategories();
        if (allowed.isEmpty()) {
            return true;
        }
        String category = article.category();
        if (category == null) {
            return false;
        }
        return allowed.stream().anyMatch(candidate -> candidate.strip().equalsIgnoreCase(category.strip()));
    }

    private static boolean hasNoExcludedKeyword(Topic topic, CandidateArticle article) {
        List<String> excluded = topic.getExcludeKeywords();
        return excluded.isEmpty()
                || (!KeywordMatcher.containsAny(article.title(), excluded)
                && !KeywordMatcher.containsAny(article.body(), excluded));
    }

    private static boolean hasRequiredKeyword(Topic topic, CandidateArticle article) {
        List<String> required = topic.getIncludeKeywords();
        return required.isEmpty()
                || KeywordMatcher.containsAny(article.title(), required)
                || KeywordMatcher.containsAny(article.body(), required);
    }
}
