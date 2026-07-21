package com.siftnews.source.adapter.out.persistence;

import com.siftnews.source.application.port.out.LoadActiveSourcesPort;
import com.siftnews.source.application.port.out.UpdateSourcePort;
import com.siftnews.source.domain.Source;
import com.siftnews.source.domain.SourceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class SourcePersistenceAdapter implements LoadActiveSourcesPort, UpdateSourcePort {

    private final SourceJpaRepository sourceJpaRepository;

    @Override
    public List<Source> loadActive() {
        return sourceJpaRepository.findByActiveTrue().stream()
                .map(SourceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Source> findActiveById(Long sourceId) {
        return sourceJpaRepository.findByIdAndActiveTrue(sourceId)
                .map(SourceMapper::toDomain);
    }

    @Override
    @Transactional
    public void markCrawled(Long sourceId, Instant at) {
        SourceJpaEntity entity = sourceJpaRepository.findById(sourceId)
                .orElseThrow(() -> new SourceException("소스를 찾을 수 없습니다: " + sourceId));
        entity.updateLastCrawledAt(at);
    }
}
