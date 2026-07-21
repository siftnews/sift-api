package com.siftnews.source.adapter.in.batch;

import com.siftnews.source.application.port.out.FetchFeedPort;
import com.siftnews.source.domain.Article;
import com.siftnews.source.domain.Source;
import org.springframework.batch.item.ItemProcessor;

import java.util.List;

/**
 * collectStep processor — 소스 하나를 받아 피드를 가져와 {@link Article} 목록(1:N)으로 변환한다.
 * 도메인 팩터리 {@link Article#create}로 url 정규화·검증을 위임한다.
 */
class SourceToArticlesProcessor implements ItemProcessor<Source, List<Article>> {

    private final FetchFeedPort fetchFeedPort;

    SourceToArticlesProcessor(FetchFeedPort fetchFeedPort) {
        this.fetchFeedPort = fetchFeedPort;
    }

    @Override
    public List<Article> process(Source source) {
        return fetchFeedPort.fetch(source).stream()
                .map(raw -> Article.create(raw, source.getSourceId()))
                .toList();
    }
}
