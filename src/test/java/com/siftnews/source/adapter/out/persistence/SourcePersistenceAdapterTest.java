package com.siftnews.source.adapter.out.persistence;

import com.siftnews.source.domain.Category;
import com.siftnews.source.domain.Source;
import com.siftnews.source.domain.SourceType;
import com.siftnews.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class SourcePersistenceAdapterTest extends AbstractIntegrationTest {

    @Autowired
    private SourceJpaRepository sourceJpaRepository;

    @Autowired
    private SourcePersistenceAdapter sourcePersistenceAdapter;

    @Test
    void loadActiveReturnsOnlyActiveSources() {
        SourceJpaEntity active = sourceJpaRepository.save(
                new SourceJpaEntity("Hacker News", SourceType.RSS, "https://news.ycombinator.com/rss", "en", Category.DEV, true, null));
        sourceJpaRepository.save(
                new SourceJpaEntity("Inactive Blog", SourceType.RSS, "https://inactive.example.com/rss", "en", Category.DEV, false, null));

        var result = sourcePersistenceAdapter.loadActive();

        assertThat(result)
                .extracting(Source::getSourceId)
                .containsExactly(active.getId());
    }

    @Test
    void findActiveByIdReturnsEmptyForInactiveSource() {
        SourceJpaEntity inactive = sourceJpaRepository.save(
                new SourceJpaEntity("Inactive", SourceType.RSS, "https://inactive.example.com/rss", "en", Category.DEV, false, null));

        Optional<Source> result = sourcePersistenceAdapter.findActiveById(inactive.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void findActiveByIdReturnsMappedDomainSourceWhenActive() {
        SourceJpaEntity entity = sourceJpaRepository.save(
                new SourceJpaEntity("Hacker News", SourceType.RSS, "https://news.ycombinator.com/rss", "en", Category.DEV, true, null));

        Optional<Source> result = sourcePersistenceAdapter.findActiveById(entity.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Hacker News");
        assertThat(result.get().getType()).isEqualTo(SourceType.RSS);
        assertThat(result.get().getUrl()).isEqualTo("https://news.ycombinator.com/rss");
        assertThat(result.get().getCategory()).isEqualTo(Category.DEV);
        assertThat(result.get().isActive()).isTrue();
    }

    @Test
    void markCrawledPersistsLastCrawledAt() {
        SourceJpaEntity entity = sourceJpaRepository.save(
                new SourceJpaEntity("Hacker News", SourceType.RSS, "https://news.ycombinator.com/rss", "en", Category.DEV, true, null));
        Instant now = Instant.parse("2026-07-13T00:00:00Z");

        sourcePersistenceAdapter.markCrawled(entity.getId(), now);

        SourceJpaEntity reloaded = sourceJpaRepository.findById(entity.getId()).orElseThrow();
        assertThat(reloaded.getLastCrawledAt()).isEqualTo(now);
    }
}
