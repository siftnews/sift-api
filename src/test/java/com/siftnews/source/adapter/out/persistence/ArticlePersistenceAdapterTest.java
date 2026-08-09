package com.siftnews.source.adapter.out.persistence;

import com.siftnews.source.domain.Article;
import com.siftnews.source.domain.Category;
import com.siftnews.source.domain.RawArticle;
import com.siftnews.support.AbstractIntegrationTest;
import com.siftnews.support.TestDatabaseFixtures;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
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

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        TestDatabaseFixtures.source(entityManager, SOURCE_ID);
    }

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

    /**
     * 한 번의 수집에 같은 기사가 여러 소스로 들어오는 일이 흔하다(신디케이션·중복 게재).
     * DB 기존분만 걸러내면 saveAll이 UNIQUE 위반을 내고 배치에서 그 소스 전체가 skip된다
     * — 2026-07-26 e2e에서 실제로 발생한 경로다.
     */
    @Test
    void saveNewDeduplicatesWithinInputList() {
        Article first = articleOf("https://example.com/a");
        Article sameArticleFromAnotherFeed = articleOf("https://example.com/a?utm_medium=rss");
        Article other = articleOf("https://example.com/b");

        int saved = articlePersistenceAdapter.saveNew(List.of(first, sameArticleFromAnotherFeed, other));

        assertThat(saved).isEqualTo(2);
        assertThat(articleJpaRepository.count()).isEqualTo(2);
    }

    /**
     * 문자열 컬럼이 JPA 기본값 varchar(255)면 실제 피드를 감당하지 못한다 — 본문 255자 초과
     * 기사 하나가 그 소스의 수집을 통째로 실패시켰다(2026-07-26 e2e, 9개 중 5개 소스).
     */
    @Test
    void saveNewPersistsLongBodyAndTitle() {
        String longBody = "본문".repeat(3000);
        String longTitle = "제".repeat(400);
        Article article = Article.create(
                new RawArticle("https://example.com/long", longTitle, longBody, "ko", PUBLISHED_AT, Category.DEV),
                SOURCE_ID);

        int saved = articlePersistenceAdapter.saveNew(List.of(article));

        assertThat(saved).isEqualTo(1);
        ArticleJpaEntity persisted = articleJpaRepository.findAll().getFirst();
        assertThat(persisted.getBody()).hasSize(longBody.length());
        assertThat(persisted.getTitle()).hasSize(longTitle.length());
    }

    private static Article articleOf(String rawUrl) {
        return Article.create(new RawArticle(rawUrl, "제목", "본문", "en", PUBLISHED_AT, Category.DEV), SOURCE_ID);
    }
}
