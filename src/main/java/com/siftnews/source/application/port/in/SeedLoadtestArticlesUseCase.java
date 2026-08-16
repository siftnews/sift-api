package com.siftnews.source.application.port.in;

public interface SeedLoadtestArticlesUseCase {

    LoadtestArticleSeedSummary seed(String runId);
}
