package com.siftnews.source.adapter.out.persistence;

/** 재정규화가 읽는 세 컬럼만 뽑는 조회 프로젝션 — 전 행 스캔에 본문을 싣지 않기 위한 것. */
interface ArticleUrlProjection {

    Long getId();

    String getUrl();

    String getNormalizedUrl();
}
