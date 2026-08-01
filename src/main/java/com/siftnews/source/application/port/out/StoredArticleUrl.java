package com.siftnews.source.application.port.out;

/** 재정규화 대상 한 행 — 원본 url과 현재 저장된 키. */
public record StoredArticleUrl(long articleId, String url, String normalizedUrl) {
}
