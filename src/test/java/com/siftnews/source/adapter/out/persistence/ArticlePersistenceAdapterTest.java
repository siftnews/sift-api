package com.siftnews.source.adapter.out.persistence;

import com.siftnews.source.domain.Article;
import com.siftnews.source.domain.Category;
import com.siftnews.source.domain.RawArticle;
import com.siftnews.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ArticlePersistenceAdapterTest extends AbstractIntegrationTest {

    private static final Instant PUBLISHED_AT = Instant.parse("2026-07-13T00:00:00Z");
    private static final Long SOURCE_ID = 1L;

    @Autowired
    private ArticleJpaRepository articleJpaRepository;

    @Autowired
    private ArticlePersistenceAdapter articlePersistenceAdapter;

    @Test
    void saveNewPersistsAllDistinctArticles() {
        Article first = articleOf("https://example.com/a");
        Article second = articleOf("https://example.com/b");

        int saved = articlePersistenceAdapter.saveNew(List.of(first, second));

        assertThat(saved).isEqualTo(2);
        assertThat(articleJpaRepository.count()).isEqualTo(2);
    }

    @Test
    void saveNewIgnoresArticlesWithAlreadyPersistedNormalizedUrl() {
        articlePersistenceAdapter.saveNew(List.of(articleOf("https://example.com/a")));

        Article duplicateByNormalizedUrl = articleOf("https://example.com/a?utm_source=news");
        Article brandNew = articleOf("https://example.com/b");

        int saved = articlePersistenceAdapter.saveNew(List.of(duplicateByNormalizedUrl, brandNew));

        assertThat(saved).isEqualTo(1);
        assertThat(articleJpaRepository.count()).isEqualTo(2);
    }

    private static Article articleOf(String rawUrl) {
        return Article.create(new RawArticle(rawUrl, "제목", "본문", "en", PUBLISHED_AT, Category.DEV), SOURCE_ID);
    }
}
