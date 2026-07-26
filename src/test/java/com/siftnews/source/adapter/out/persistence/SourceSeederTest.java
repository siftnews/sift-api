package com.siftnews.source.adapter.out.persistence;

import com.siftnews.source.domain.Category;
import com.siftnews.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 시더는 {@code @Profile("!test")}라 테스트 컨텍스트에 빈으로 뜨지 않으므로,
 * 직접 생성해 Testcontainers DB에 대해 시드·멱등을 검증한다.
 */
@Transactional
class SourceSeederTest extends AbstractIntegrationTest {

    @Autowired
    private SourceJpaRepository sourceJpaRepository;

    @Test
    void seedsNineSourcesIdempotently() {
        SourceSeeder seeder = new SourceSeeder(sourceJpaRepository);

        seeder.run(null);

        assertThat(sourceJpaRepository.count()).isEqualTo(9);
        assertThat(sourceJpaRepository.existsByUrl("https://news.ycombinator.com/rss")).isTrue();
        assertThat(sourceJpaRepository.existsByUrl("https://www.aitimes.com/rss/allArticle.xml")).isTrue();
        assertThat(sourceJpaRepository.existsByUrl("https://feeds.bbci.co.uk/news/business/rss.xml")).isTrue();

        seeder.run(null);

        assertThat(sourceJpaRepository.count()).isEqualTo(9);
    }

    /**
     * 시드된 소스는 전부 active여야 collectionJob의 reader({@code findByActiveTrue})가 집어간다 —
     * 하나라도 비활성이면 그 피드는 조용히 수집에서 빠진다.
     */
    @Test
    void seedsAllSourcesActiveAndUncrawled() {
        new SourceSeeder(sourceJpaRepository).run(null);

        assertThat(sourceJpaRepository.findByActiveTrue()).hasSize(9)
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
        new SourceSeeder(sourceJpaRepository).run(null);

        assertThat(sourceJpaRepository.findByActiveTrue())
                .extracting(SourceJpaEntity::getCategory)
                .contains(Category.DEV, Category.PROGRAMMING, Category.AI, Category.ECONOMY, Category.FINANCE);
    }
}
