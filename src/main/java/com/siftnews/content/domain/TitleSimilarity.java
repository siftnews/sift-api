package com.siftnews.content.domain;

import java.util.Arrays;
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
        return jaccard(tokenize(a), tokenize(b));
    }

    static double jaccard(Set<String> ta, Set<String> tb) {
        if (ta.isEmpty() || tb.isEmpty()) {
            return 0.0;
        }
        Set<String> smaller = ta.size() <= tb.size() ? ta : tb;
        Set<String> larger = smaller == ta ? tb : ta;
        int intersectionSize = 0;
        for (String token : smaller) {
            if (larger.contains(token)) {
                intersectionSize++;
            }
        }
        int unionSize = ta.size() + tb.size() - intersectionSize;
        return (double) intersectionSize / unionSize;
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
