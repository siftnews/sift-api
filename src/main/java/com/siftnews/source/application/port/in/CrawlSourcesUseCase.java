package com.siftnews.source.application.port.in;

public interface CrawlSourcesUseCase {

    void crawlAll();

    void crawl(Long sourceId);
}
