package com.siftnews.source.application.service;

import com.siftnews.source.application.port.in.RenormalizeSummary;
import com.siftnews.source.application.port.out.ArticleUrlPort;
import com.siftnews.source.application.port.out.StoredArticleUrl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RenormalizeArticleUrlsServiceTest {

    private final FakeArticleUrlPort articleUrlPort = new FakeArticleUrlPort();
    private final RenormalizeArticleUrlsService service = new RenormalizeArticleUrlsService(articleUrlPort);

    @Test
    void renormalizeMovesRowsLeftOnTheOldKey() {
        // 옛 규칙(쿼리 전량 제거)으로 저장된 행 — 쿼리로 기사를 구분하는 소스가 이 꼴로 남아 있다
        articleUrlPort.store(1L, "https://www.aitimes.com/news/articleView.html?idxno=213427",
                "https://www.aitimes.com/news/articleView.html");

        RenormalizeSummary summary = service.renormalize();

        assertThat(summary.changed()).isEqualTo(1);
        assertThat(articleUrlPort.normalizedUrlOf(1L))
                .isEqualTo("https://www.aitimes.com/news/articleView.html?idxno=213427");
    }

    @Test
    void renormalizeLeavesRowsThatAlreadyMatchTheRule() {
        articleUrlPort.store(1L, "https://toss.tech/article/x", "https://toss.tech/article/x");
        // 추적 파라미터만 달린 소스는 새 규칙에서도 키가 같다 — 백필 대상이 아니다
        articleUrlPort.store(2L, "https://www.bbc.co.uk/news/articles/abc?at_medium=RSS",
                "https://www.bbc.co.uk/news/articles/abc");

        RenormalizeSummary summary = service.renormalize();

        assertThat(summary.unchanged()).isEqualTo(2);
        assertThat(summary.changed()).isZero();
    }

    /** 두 번째 실행은 전부 unchanged — 매 기동마다 돌아도 결과가 덧나지 않아야 한다. */
    @Test
    void renormalizeIsIdempotent() {
        articleUrlPort.store(1L, "https://www.aitimes.com/news/articleView.html?idxno=213427",
                "https://www.aitimes.com/news/articleView.html");
        service.renormalize();

        RenormalizeSummary second = service.renormalize();

        assertThat(second.scanned()).isEqualTo(1);
        assertThat(second.changed()).isZero();
        assertThat(second.unchanged()).isEqualTo(1);
    }

    /** 새 키를 이미 다른 행이 점유했으면 건너뛰고 센다 — 같은 기사가 두 행이라는 신호다. */
    @Test
    void renormalizeCountsConflictWithoutTouchingTheOccupiedRow() {
        articleUrlPort.store(1L, "https://ex.com/news?id=7", "https://ex.com/news");
        articleUrlPort.store(2L, "https://ex.com/news?id=7", "https://ex.com/news?id=7");

        RenormalizeSummary summary = service.renormalize();

        assertThat(summary.conflicted()).isEqualTo(1);
        assertThat(articleUrlPort.normalizedUrlOf(1L)).isEqualTo("https://ex.com/news");
        assertThat(articleUrlPort.normalizedUrlOf(2L)).isEqualTo("https://ex.com/news?id=7");
    }

    @Test
    void renormalizeSkipsRowWhoseStoredUrlCannotBeNormalized() {
        articleUrlPort.store(1L, "not a url", "https://ex.com/broken");
        articleUrlPort.store(2L, "https://ex.com/news?id=7", "https://ex.com/news");

        RenormalizeSummary summary = service.renormalize();

        assertThat(summary.invalid()).isEqualTo(1);
        assertThat(summary.changed()).isEqualTo(1);
        assertThat(articleUrlPort.normalizedUrlOf(1L)).isEqualTo("https://ex.com/broken");
    }

    /** 페이지 크기(500)를 넘겨도 커서가 끝까지 진행해야 한다 — 한 페이지에서 멈추면 나머지가 옛 키로 남는다. */
    @Test
    void renormalizeWalksEveryPage() {
        int total = 1200;
        for (int i = 1; i <= total; i++) {
            articleUrlPort.store(i, "https://ex.com/news?id=" + i, "https://ex.com/news/" + i);
        }

        RenormalizeSummary summary = service.renormalize();

        assertThat(summary.scanned()).isEqualTo(total);
        assertThat(summary.changed()).isEqualTo(total);
        assertThat(articleUrlPort.normalizedUrlOf(total)).isEqualTo("https://ex.com/news?id=" + total);
    }

    @Test
    void renormalizeOnEmptyTableReportsNothing() {
        RenormalizeSummary summary = service.renormalize();

        assertThat(summary).isEqualTo(RenormalizeSummary.empty());
    }

    private static class FakeArticleUrlPort implements ArticleUrlPort {

        private final Map<Long, StoredArticleUrl> rows = new LinkedHashMap<>();

        void store(long articleId, String url, String normalizedUrl) {
            rows.put(articleId, new StoredArticleUrl(articleId, url, normalizedUrl));
        }

        String normalizedUrlOf(long articleId) {
            return rows.get(articleId).normalizedUrl();
        }

        @Override
        public List<StoredArticleUrl> findUrlsAfter(long afterId, int limit) {
            return rows.values().stream()
                    .filter(row -> row.articleId() > afterId)
                    .sorted(Comparator.comparingLong(StoredArticleUrl::articleId))
                    .limit(limit)
                    .toList();
        }

        @Override
        public boolean updateNormalizedUrl(long articleId, String normalizedUrl) {
            Set<String> occupied = new HashSet<>();
            for (StoredArticleUrl row : new ArrayList<>(rows.values())) {
                if (row.articleId() != articleId) {
                    occupied.add(row.normalizedUrl());
                }
            }
            if (occupied.contains(normalizedUrl)) {
                return false;
            }

            StoredArticleUrl row = rows.get(articleId);
            rows.put(articleId, new StoredArticleUrl(articleId, row.url(), normalizedUrl));
            return true;
        }
    }
}
