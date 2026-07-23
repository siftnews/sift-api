package com.siftnews.content.domain;

import java.util.Locale;
import java.util.Set;

/**
 * Normalize 컷 규칙 (SELECTION §2.1) — 후보로 살아남을지 판정한다.
 * <p>
 * 전역 1회 단계라 토픽을 모른다. 지원 언어 밖이거나 본문이 최소 길이 미만이면 drop.
 * 임계값은 MVP 기본값 — breakdown 로그를 보며 튜닝한다.
 */
public final class ArticleNormalizer {

    static final int MIN_BODY_LENGTH = 200;
    static final Set<String> SUPPORTED_LANGS = Set.of("ko", "en");

    private ArticleNormalizer() {
    }

    public static boolean survives(CandidateArticle article) {
        String lang = article.lang();
        if (lang == null || !SUPPORTED_LANGS.contains(lang.toLowerCase(Locale.ROOT))) {
            return false;
        }
        String body = article.body();
        return body != null && body.strip().length() >= MIN_BODY_LENGTH;
    }
}
