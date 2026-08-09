package com.siftnews.source.adapter.in.bootstrap;

import com.siftnews.source.application.port.in.SeedSourcesUseCase;
import com.siftnews.source.application.port.out.LoadActiveSourcesPort;
import com.siftnews.source.domain.Category;
import com.siftnews.source.domain.Source;
import com.siftnews.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 시더는 {@code @Profile("!test")}라 테스트 컨텍스트에 빈으로 뜨지 않으므로,
 * 직접 생성해 Testcontainers DB에 대해 시드·멱등을 검증한다.
 */
@Transactional
class SourceSeederTest extends AbstractIntegrationTest {

    @Autowired
    private SeedSourcesUseCase seedSourcesUseCase;

    @Autowired
    private LoadActiveSourcesPort loadActiveSourcesPort;

    @Test
    void seedsTenSourcesIdempotently() {
        runSeeder();

        assertThat(loadActiveSourcesPort.loadActive())
                .hasSize(10)
                .extracting(Source::getUrl)
                .contains("https://news.ycombinator.com/rss",
                        "https://d2.naver.com/d2.atom",
                        "https://www.aitimes.com/rss/allArticle.xml",
                        "https://feeds.bbci.co.uk/news/business/rss.xml");

        runSeeder();

        assertThat(loadActiveSourcesPort.loadActive()).hasSize(10);
    }

    /**
     * 재기동은 <b>예외 없이 0건</b>이어야 한다 — 저장이 conflict-ignore가 아니면 유니크 제약에
     * 걸려 두 번째 기동이 죽는다(두 인스턴스가 동시에 기동할 때 한쪽이 죽는 것과 같은 경로).
     */
    @Test
    void reseedInsertsNothing() {
        List<Source> sources = SourceSeedData.sources();

        assertThat(seedSourcesUseCase.seed(sources)).isEqualTo(10);
        assertThat(seedSourcesUseCase.seed(sources)).isZero();
    }

    /**
     * 시드된 소스는 전부 active여야 collectionJob의 reader({@code findByActiveTrue})가 집어간다 —
     * 하나라도 비활성이면 그 피드는 조용히 수집에서 빠진다.
     */
    @Test
    void seedsAllSourcesActiveAndUncrawled() {
        runSeeder();

        assertThat(loadActiveSourcesPort.loadActive()).hasSize(10)
                .allSatisfy(source -> {
                    assertThat(source.isActive()).isTrue();
                    assertThat(source.getLastCrawledAt()).isNull();
                });
    }

    /**
     * 토픽이 후보를 끌어올 때 쓰는 카테고리 태그와 대응하는지 — 세 토픽 모두 소스가 있어야
     * 선별 단계에서 빈 후보로 떨어지지 않는다(TopicSeedData의 sourceCategories).
     */
    @Test
    void seedsCoverAllThreeTopicCategories() {
        runSeeder();

        assertThat(loadActiveSourcesPort.loadActive())
                .extracting(Source::getCategory)
                .contains(Category.DEV, Category.PROGRAMMING, Category.AI, Category.ECONOMY, Category.FINANCE);
    }

    private void runSeeder() {
        new SourceSeeder(seedSourcesUseCase).run(null);
    }
}
