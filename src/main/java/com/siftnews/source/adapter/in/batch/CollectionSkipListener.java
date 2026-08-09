package com.siftnews.source.adapter.in.batch;

import com.siftnews.source.domain.Article;
import com.siftnews.source.domain.Source;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;

import java.util.List;

/**
 * collectStep의 skip 사유를 남긴다.
 * <p>
 * {@code .skip(Exception.class)}는 소스별 오류를 격리해주지만 <b>예외를 삼키기만 해서</b>
 * 어떤 소스가 왜 빠졌는지 알 수 없었다 — 2026-07-26 e2e에서 9개 소스 중 2개가 원인 불명으로
 * 누락됐고, 그런데도 Job status는 {@code COMPLETED}로 떴다. skip이 일어난 시점에는 이 로그가
 * <b>유일한 실패 신호</b>이므로 소스를 식별할 수 있게 남긴다.
 */
@Slf4j
class CollectionSkipListener implements SkipListener<Source, List<Article>> {

    @Override
    public void onSkipInRead(Throwable t) {
        log.warn("collectStep 소스 읽기 skip", t);
    }

    @Override
    public void onSkipInProcess(Source source, Throwable t) {
        log.warn("collectStep 수집 skip: source={}(id={}) url={}",
                source.getName(), source.getSourceId(), source.getUrl(), t);
    }

    /**
     * 기사 url 대신 {@code sourceId}만 남긴다 — 어느 소스가 누락됐는지 식별하기에 충분하면서
     * 로그 카디널리티가 소스 수로 제한된다(기사 단위로 남기면 chunk 크기만큼 불어난다).
     */
    @Override
    public void onSkipInWrite(List<Article> articles, Throwable t) {
        List<Long> sourceIds = articles.stream()
                .map(Article::getSourceId)
                .distinct()
                .toList();
        log.warn("collectStep 저장 skip: 기사 {}건 sourceIds={}", articles.size(), sourceIds, t);
    }
}
