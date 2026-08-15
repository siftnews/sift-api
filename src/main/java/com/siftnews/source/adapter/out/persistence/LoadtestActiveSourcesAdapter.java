package com.siftnews.source.adapter.out.persistence;

import com.siftnews.source.application.port.out.LoadActiveSourcesPort;
import com.siftnews.source.domain.Source;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * loadtest에서는 실 운영 소스를 collectionJob 대상에서 제외하고 fixture source만 반환한다.
 */
@Component
@Primary
@Profile("loadtest & !test")
class LoadtestActiveSourcesAdapter implements LoadActiveSourcesPort {

    static final String FIXTURE_SOURCE_URL_PREFIX = "https://loadtest.sift.local/news/";
    private static final Pattern FIXTURE_SOURCE_NAME = Pattern.compile("source-\\d+\\.xml");

    private final SourceJpaRepository sourceJpaRepository;
    private final String sourceUrlPrefix;

    LoadtestActiveSourcesAdapter(
            SourceJpaRepository sourceJpaRepository,
            @Value("${sift.load-test.news.source-url-prefix:https://loadtest.sift.local/news/}")
            String sourceUrlPrefix) {
        this.sourceJpaRepository = sourceJpaRepository;
        this.sourceUrlPrefix = validateSourceUrlPrefix(sourceUrlPrefix);
    }

    @Override
    public List<Source> loadActive() {
        return sourceJpaRepository.findByActiveTrueAndUrlStartingWith(sourceUrlPrefix).stream()
                .filter(entity -> isFixtureSourceUrl(entity.getUrl(), sourceUrlPrefix))
                .sorted(Comparator.comparing(SourceJpaEntity::getUrl))
                .map(SourceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Source> findActiveById(Long sourceId) {
        return sourceJpaRepository.findByIdAndActiveTrue(sourceId)
                .filter(entity -> isFixtureSourceUrl(entity.getUrl(), sourceUrlPrefix))
                .map(SourceMapper::toDomain);
    }

    static boolean isFixtureSourceUrl(String sourceUrl, String sourceUrlPrefix) {
        if (sourceUrl == null || !sourceUrl.startsWith(sourceUrlPrefix)) {
            return false;
        }
        return FIXTURE_SOURCE_NAME.matcher(sourceUrl.substring(sourceUrlPrefix.length())).matches();
    }

    private static String validateSourceUrlPrefix(String sourceUrlPrefix) {
        if (!FIXTURE_SOURCE_URL_PREFIX.equals(sourceUrlPrefix)) {
            throw new IllegalArgumentException("loadtest source-url-prefix는 예약된 fixture namespace여야 합니다: "
                    + FIXTURE_SOURCE_URL_PREFIX);
        }
        return sourceUrlPrefix;
    }
}
