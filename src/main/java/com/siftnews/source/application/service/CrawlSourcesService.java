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
import java.util.ArrayList;
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
        List<Long> failedSourceIds = new ArrayList<>();
        for (Source source : loadActiveSourcesPort.loadActive()) {
            try {
                crawlSource(source);
            } catch (Exception e) {
                log.error("소스 크롤링 상태 갱신 실패: sourceId={}, name={}", source.getSourceId(), source.getName(), e);
                failedSourceIds.add(source.getSourceId());
            }
        }
        if (!failedSourceIds.isEmpty()) {
            throw new SourceException("크롤링 상태 갱신 실패: sourceId=" + failedSourceIds);
        }
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
        List<Article> articles;
        try {
            articles = fetchFeedPort.fetch(source).stream()
                    .map(raw -> Article.create(raw, source.getSourceId()))
                    .toList();
            saveArticlePort.saveNew(articles);
        } catch (Exception e) {
            log.warn("소스 크롤링 실패: sourceId={}, name={}", source.getSourceId(), source.getName(), e);
            return;
        }
        Instant now = clock.instant();
        source.markCrawled(now);
        updateSourcePort.markCrawled(source.getSourceId(), now);
    }
}
