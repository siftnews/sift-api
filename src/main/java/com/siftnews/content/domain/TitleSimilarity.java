package com.siftnews.content.domain;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 제목 토큰 Jaccard 유사도 (D-030) — dedup 2차 판정에 쓴다.
 * <p>
 * 유니코드 문자·숫자만 토큰으로 보고(구두점·공백 분리) 소문자화한 집합의
 * 교집합/합집합 비율을 반환한다. SimHash는 대규모 전환 시 재검토(성능 로드맵).
 */
public final class TitleSimilarity {

    private TitleSimilarity() {
    }

    public static double jaccard(String a, String b) {
        Set<String> ta = tokenize(a);
        Set<String> tb = tokenize(b);
        if (ta.isEmpty() || tb.isEmpty()) {
            return 0.0;
        }
        Set<String> intersection = new HashSet<>(ta);
        intersection.retainAll(tb);
        Set<String> union = new HashSet<>(ta);
        union.addAll(tb);
        return (double) intersection.size() / union.size();
    }

    static Set<String> tokenize(String s) {
        if (s == null || s.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(s.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
                .filter(token -> !token.isBlank())
                .collect(Collectors.toSet());
    }
}
