package com.siftnews.source.adapter.out.persistence;

import com.siftnews.source.domain.Category;
import com.siftnews.source.domain.Source;
import com.siftnews.source.domain.SourceType;
import com.siftnews.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class LoadtestActiveSourcesAdapterTest extends AbstractIntegrationTest {

    @Autowired
    private SourceJpaRepository sourceJpaRepository;

    @Test
    void loadsOnlyReservedFixtureSourcesWhenOperationalSourcesAlsoExist() {
        sourceJpaRepository.saveAll(List.of(
                new SourceJpaEntity("fixture", SourceType.RSS,
                        LoadtestActiveSourcesAdapter.FIXTURE_SOURCE_URL_PREFIX + "source-00.xml",
                        "en", Category.DEV, true, null),
                new SourceJpaEntity("operational", SourceType.RSS,
                        "https://news.example.com/rss.xml", "en", Category.DEV, true, null),
                new SourceJpaEntity("wrong fixture shape", SourceType.RSS,
                        LoadtestActiveSourcesAdapter.FIXTURE_SOURCE_URL_PREFIX + "not-a-fixture.xml",
                        "en", Category.DEV, true, null)));
        sourceJpaRepository.flush();

        LoadtestActiveSourcesAdapter adapter = new LoadtestActiveSourcesAdapter(
                sourceJpaRepository, LoadtestActiveSourcesAdapter.FIXTURE_SOURCE_URL_PREFIX);

        assertThat(adapter.loadActive())
                .extracting(Source::getUrl)
                .containsExactly(LoadtestActiveSourcesAdapter.FIXTURE_SOURCE_URL_PREFIX + "source-00.xml");
    }

    @Test
    void rejectsAnUnreservedFixtureNamespaceAtStartup() {
        assertThatThrownBy(() -> new LoadtestActiveSourcesAdapter(sourceJpaRepository, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("예약된 fixture namespace");
    }
}
