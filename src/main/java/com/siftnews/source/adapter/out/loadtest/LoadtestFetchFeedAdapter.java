package com.siftnews.source.adapter.out.loadtest;

import com.siftnews.source.application.port.out.FetchFeedPort;
import com.siftnews.source.domain.RawArticle;
import com.siftnews.source.domain.Source;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * 외부 RSS에 의존하지 않는 collectionJob fixture feed.
 * <p>
 * source 하나당 URL이 서로 다른 고정 기사 N건을 생성하므로, 네트워크 지연·피드 변경 없이
 * 같은 배치 경로의 수집/중복 저장 비용을 반복 측정할 수 있다.
 */
@Component
@Profile("loadtest & !test")
class LoadtestFetchFeedAdapter implements FetchFeedPort {

    private static final Pattern SOURCE_NUMBER = Pattern.compile("source-(\\d+)\\.xml$");

    private final Instant fixturePublishedAt;
    private final int entriesPerSource;
    private final String articleUrlPrefix;

    LoadtestFetchFeedAdapter(
            @Value("${sift.load-test.news.collection.fixture-published-at:2026-01-01T00:00:00Z}")
            Instant fixturePublishedAt,
            @Value("${sift.load-test.news.collection.entries-per-source:1000}") int entriesPerSource,
            @Value("${sift.load-test.news.collection.article-url-prefix:https://loadtest.sift.local/collection/articles/}")
            String articleUrlPrefix) {
        if (entriesPerSource <= 0) {
            throw new IllegalArgumentException("collection fixture entries-per-source는 양수여야 합니다.");
        }
        this.fixturePublishedAt = fixturePublishedAt;
        this.entriesPerSource = entriesPerSource;
        this.articleUrlPrefix = articleUrlPrefix;
    }

    @Override
    public List<RawArticle> fetch(Source source) {
        int sourceNumber = sourceNumber(source);
        String sourceKey = "%02d".formatted(sourceNumber);
        String runId = runId();
        String title = "Spring loadtest source" + sourceKey + " article";
        String body = body(sourceKey);

        return IntStream.range(0, entriesPerSource)
                .mapToObj(index -> new RawArticle(
                        articleUrlPrefix + runId + "/source-" + sourceKey + "/article-" + "%04d".formatted(index),
                        title,
                        body,
                        source.getLang(),
                        fixturePublishedAt,
                        source.getCategory()))
                .toList();
    }

    private static int sourceNumber(Source source) {
        Matcher matcher = SOURCE_NUMBER.matcher(source.getUrl());
        if (!matcher.find()) {
            throw new IllegalArgumentException("loadtest source URL에서 source 번호를 읽을 수 없습니다: " + source.getUrl());
        }
        return Integer.parseInt(matcher.group(1));
    }

    private static String runId() {
        if (StepSynchronizationManager.getContext() == null) {
            return "direct";
        }
        String runId = StepSynchronizationManager.getContext().getStepExecution()
                .getJobParameters().getString("runId");
        return runId == null || runId.isBlank() ? "default" : runId;
    }

    private static String body(String sourceKey) {
        return ("Spring loadtest fixture source=" + sourceKey
                + "; deterministic article body for collection and selection measurement. ").repeat(8);
    }
}
