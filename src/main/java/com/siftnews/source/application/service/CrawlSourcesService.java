package com.siftnews.source.application.service;

import com.siftnews.source.application.port.in.CrawlSourcesUseCase;
import com.siftnews.source.application.port.out.FetchFeedPort;
import com.siftnews.source.application.port.out.LoadActiveSourcesPort;
import com.siftnews.source.application.port.out.SaveArticlePort;
import com.siftnews.source.application.port.out.UpdateSourcePort;
import com.siftnews.source.domain.Article;
import com.siftnews.source.domain.Source;
import com.siftnews.source.domain.SourceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class CrawlSourcesService implements CrawlSourcesUseCase {

    private final LoadActiveSourcesPort loadActiveSourcesPort;
    private final FetchFeedPort fetchFeedPort;
    private final SaveArticlePort saveArticlePort;
    private final UpdateSourcePort updateSourcePort;
    private final Clock clock;

    @Override
    public void crawlAll() {
        loadActiveSourcesPort.loadActive().forEach(this::crawlSource);
    }

    @Override
    public void crawl(Long sourceId) {
        crawlSource(findActiveSource(sourceId));
    }

    private Source findActiveSource(Long sourceId) {
        return loadActiveSourcesPort.findActiveById(sourceId)
                .orElseThrow(() -> new SourceException("활성 소스를 찾을 수 없습니다: " + sourceId));
    }

    private void crawlSource(Source source) {
        try {
            List<Article> articles = fetchFeedPort.fetch(source).stream()
                    .map(raw -> Article.create(raw, source.getSourceId()))
                    .toList();
            saveArticlePort.saveNew(articles);
            Instant now = clock.instant();
            source.markCrawled(now);
            updateSourcePort.markCrawled(source.getSourceId(), now);
        } catch (Exception e) {
            log.warn("소스 크롤링 실패: sourceId={}, name={}", source.getSourceId(), source.getName(), e);
        }
    }
}
