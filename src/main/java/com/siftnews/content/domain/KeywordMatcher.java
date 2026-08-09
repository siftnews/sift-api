package com.siftnews.content.domain;

import java.util.List;
import java.util.Locale;

/**
 * 토픽 키워드가 기사 텍스트에 나타나는지 판정한다 — Filter(포함/제외)와 Score(가중합)가 함께 쓴다.
 * <p>
 * MVP는 <b>대소문자 무시 부분 문자열 매칭</b>이다. 한국어는 대소문자가 없고 조사가 붙어
 * ("금리가", "금리는") 단어 경계로 자르면 오히려 놓치기 때문이다. 대신 영어에서
 * {@code "Java"}가 {@code "JavaScript"}에 걸리는 과매칭이 생긴다 — 알고 수용하는 값이며,
 * breakdown 로그로 실제 오탐 빈도를 본 뒤 형태소·단어 경계 도입을 판단한다(SELECTION §5).
 */
final class KeywordMatcher {

    private KeywordMatcher() {
    }

    static boolean contains(String text, String keyword) {
        if (text == null || keyword == null || keyword.isBlank()) {
            return false;
        }
        return text.toLowerCase(Locale.ROOT).contains(keyword.strip().toLowerCase(Locale.ROOT));
    }

    static boolean containsAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(keyword -> contains(text, keyword));
    }
}
