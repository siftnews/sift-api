package com.siftnews.source.adapter.out.persistence;

import com.siftnews.source.application.port.out.StoredArticleUrl;
import com.siftnews.source.domain.Category;
import com.siftnews.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ArticleUrlAdapterTest extends AbstractIntegrationTest {

    private static final Long SOURCE_ID = 1L;

    @Autowired
    private ArticleJpaRepository articleJpaRepository;

    @Autowired
    private ArticleUrlAdapter articleUrlAdapter;

    @Test
    void findUrlsAfterReturnsRowsInIdOrderWithinLimit() {
        Long first = persist("https://ex.com/a?id=1", "https://ex.com/a");
        Long second = persist("https://ex.com/b?id=2", "https://ex.com/b");
        persist("https://ex.com/c?id=3", "https://ex.com/c");

        List<StoredArticleUrl> page = articleUrlAdapter.findUrlsAfter(0L, 2);

        assertThat(page).extracting(StoredArticleUrl::articleId).containsExactly(first, second);
        assertThat(page.get(0).url()).isEqualTo("https://ex.com/a?id=1");
        assertThat(page.get(0).normalizedUrl()).isEqualTo("https://ex.com/a");
    }

    @Test
    void findUrlsAfterAdvancesPastTheCursor() {
        Long first = persist("https://ex.com/a?id=1", "https://ex.com/a");
        Long second = persist("https://ex.com/b?id=2", "https://ex.com/b");

        List<StoredArticleUrl> page = articleUrlAdapter.findUrlsAfter(first, 10);

        assertThat(page).extracting(StoredArticleUrl::articleId).containsExactly(second);
    }

    @Test
    void updateNormalizedUrlMovesRowToTheNewKey() {
        Long articleId = persist("https://ex.com/a?id=1", "https://ex.com/a");

        boolean updated = articleUrlAdapter.updateNormalizedUrl(articleId, "https://ex.com/a?id=1");

        assertThat(updated).isTrue();
        assertThat(articleJpaRepository.findById(articleId))
                .get()
                .extracting(ArticleJpaEntity::getNormalizedUrl)
                .isEqualTo("https://ex.com/a?id=1");
    }

    /**
     * 점유된 키로는 갱신하지 않는다 — {@code UNIQUE(normalized_url)} 위반 예외가 아니라 false로
     * 돌아와야, 한 건의 충돌이 앞서 옮긴 행들을 되돌리지 않는다.
     */
    @Test
    void updateNormalizedUrlReturnsFalseWhenTargetKeyIsTaken() {
        Long stale = persist("https://ex.com/a?id=1", "https://ex.com/a");
        persist("https://ex.com/a?id=1", "https://ex.com/a?id=1");

        boolean updated = articleUrlAdapter.updateNormalizedUrl(stale, "https://ex.com/a?id=1");

        assertThat(updated).isFalse();
        assertThat(articleJpaRepository.findById(stale))
                .get()
                .extracting(ArticleJpaEntity::getNormalizedUrl)
                .isEqualTo("https://ex.com/a");
    }

    private Long persist(String url, String normalizedUrl) {
        ArticleJpaEntity entity = new ArticleJpaEntity(SOURCE_ID, url, normalizedUrl, "제목", "본문",
                "ko", Instant.parse("2026-08-01T00:00:00Z"), Category.AI);
        return articleJpaRepository.save(entity).getId();
    }
}
