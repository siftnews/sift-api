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

/**
 * loadtest에서는 실 운영 소스를 collectionJob 대상에서 제외하고 fixture source만 반환한다.
 */
@Component
@Primary
@Profile("loadtest & !test")
class LoadtestActiveSourcesAdapter implements LoadActiveSourcesPort {

    private final SourceJpaRepository sourceJpaRepository;
    private final String sourceUrlPrefix;

    LoadtestActiveSourcesAdapter(
            SourceJpaRepository sourceJpaRepository,
            @Value("${sift.load-test.news.source-url-prefix:https://loadtest.sift.local/news/}")
            String sourceUrlPrefix) {
        this.sourceJpaRepository = sourceJpaRepository;
        this.sourceUrlPrefix = sourceUrlPrefix;
    }

    @Override
    public List<Source> loadActive() {
        return sourceJpaRepository.findByActiveTrueAndUrlStartingWith(sourceUrlPrefix).stream()
                .sorted(Comparator.comparing(SourceJpaEntity::getUrl))
                .map(SourceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Source> findActiveById(Long sourceId) {
        return sourceJpaRepository.findByIdAndActiveTrue(sourceId)
                .filter(entity -> entity.getUrl().startsWith(sourceUrlPrefix))
                .map(SourceMapper::toDomain);
    }
}
