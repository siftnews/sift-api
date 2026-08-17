package com.siftnews.source.application.service;

import com.siftnews.source.application.port.in.LoadtestArticleSeedSummary;
import com.siftnews.source.application.port.in.SeedLoadtestArticlesUseCase;
import com.siftnews.source.application.port.out.LoadActiveSourcesPort;
import com.siftnews.source.application.port.out.SaveArticlePort;
import com.siftnews.source.domain.Article;
import com.siftnews.source.domain.RawArticle;
import com.siftnews.source.domain.Source;
import com.siftnews.source.domain.SourceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * selectionJob 입력 후보를 별도 HTTP 시드 단계로 준비한다.
 * <p>
 * collectionJob이 만든 기사와 URL namespace를 분리하고, 시드 시작 시각을 윈도우 하한으로
 * 반환한다. 따라서 기존 데이터를 삭제하지 않고도 같은 staging DB에서 수집과 선별 측정을
 * 순차 실행할 수 있다. 기사 저장은 Source 소유 {@link SaveArticlePort}를 사용한다.
 */
@Slf4j
@Service
@Profile("loadtest & !test")
class LoadtestArticleSeedService implements SeedLoadtestArticlesUseCase {

    private static final Pattern RUN_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private final LoadActiveSourcesPort loadActiveSourcesPort;
    private final SaveArticlePort saveArticlePort;
    private final Clock clock;
    private final int expectedSourceCount;
    private final int candidateCount;
    private final Duration windowSize;
    private final int saveChunkSize;
    private final String articleUrlPrefix;

    LoadtestArticleSeedService(
            LoadActiveSourcesPort loadActiveSourcesPort,
            SaveArticlePort saveArticlePort,
            Clock clock,
            @Value("${sift.load-test.news.source-count:10}") int expectedSourceCount,
            @Value("${sift.load-test.news.selection.candidate-count:10000}") int candidateCount,
            @Value("${sift.load-test.news.selection.window-hours:24}") int windowHours,
            @Value("${sift.load-test.news.selection.save-chunk-size:500}") int saveChunkSize,
            @Value("${sift.load-test.news.selection.article-url-prefix:https://loadtest.sift.local/selection/articles/}")
            String articleUrlPrefix) {
        if (expectedSourceCount <= 0 || candidateCount <= 0 || windowHours <= 0 || saveChunkSize <= 0) {
            throw new IllegalArgumentException("selection fixture 설정은 모두 양수여야 합니다.");
        }
        this.loadActiveSourcesPort = loadActiveSourcesPort;
        this.saveArticlePort = saveArticlePort;
        this.clock = clock;
        this.expectedSourceCount = expectedSourceCount;
        this.candidateCount = candidateCount;
        this.windowSize = Duration.ofHours(windowHours);
        this.saveChunkSize = saveChunkSize;
        this.articleUrlPrefix = articleUrlPrefix;
    }

    @Override
    public LoadtestArticleSeedSummary seed(String runId) {
        validateRunId(runId);

        List<Source> sources = loadActiveSourcesPort.loadActive().stream()
                .sorted(Comparator.comparing(Source::getUrl))
                .toList();
        if (sources.size() != expectedSourceCount) {
            throw new IllegalStateException("selection fixture source 수가 예상과 다릅니다: expected="
                    + expectedSourceCount + ", actual=" + sources.size());
        }
        if (candidateCount % sources.size() != 0) {
            throw new IllegalStateException("selection fixture candidate-count는 source 수로 나누어 떨어져야 합니다.");
        }

        Instant windowFrom = clock.instant();
        int perSource = candidateCount / sources.size();
        int inserted = 0;
        for (int sourceIndex = 0; sourceIndex < sources.size(); sourceIndex++) {
            Source source = sources.get(sourceIndex);
            List<Article> articles = new ArrayList<>(perSource);
            for (int articleIndex = 0; articleIndex < perSource; articleIndex++) {
                articles.add(article(source, runId, sourceIndex, articleIndex, windowFrom));
            }
            inserted += saveInChunks(articles);
        }

        Instant windowTo = windowFrom.plus(windowSize);
        log.info("[loadtest] selection fixture 시드 완료: runId={} requested={} inserted={} sourceCount={} window=[{}, {})",
                runId, candidateCount, inserted, sources.size(), windowFrom, windowTo);
        return new LoadtestArticleSeedSummary(runId, candidateCount, inserted, sources.size(), windowFrom, windowTo);
    }

    private int saveInChunks(List<Article> articles) {
        int inserted = 0;
        for (int from = 0; from < articles.size(); from += saveChunkSize) {
            int to = Math.min(from + saveChunkSize, articles.size());
            inserted += saveArticlePort.saveNew(articles.subList(from, to));
        }
        return inserted;
    }

    private Article article(Source source, String runId, int sourceIndex, int articleIndex, Instant windowFrom) {
        String sourceKey = "%02d".formatted(sourceIndex);
        String articleKey = "%04d".formatted(articleIndex);
        // 각 후보가 서로 다른 뉴스처럼 동작하도록 제목 signature를 article 단위로 만든다.
        // 공통 토큰은 Jaccard threshold(0.7) 미만으로만 유지해 10,000건이 한 cluster로 합쳐지지 않게 한다.
        String title = "Spring story source" + sourceKey + " entry" + articleKey + " signature";
        String url = articleUrlPrefix + runId + "/source-" + sourceKey + "/article-" + articleKey;
        String body = ("Spring selection loadtest fixture source=" + sourceKey
                + " article=" + articleKey + "; deterministic unique candidate body for a 10,000 article workload. ").repeat(8);
        RawArticle raw = new RawArticle(url, title, body, source.getLang(),
                windowFrom.minusSeconds(articleIndex), source.getCategory());
        return Article.create(raw, source.getSourceId());
    }

    private static void validateRunId(String runId) {
        if (runId == null || !RUN_ID_PATTERN.matcher(runId).matches()) {
            throw new SourceException("runId는 영문·숫자·하이픈·밑줄 1~64자여야 합니다.");
        }
    }
}
