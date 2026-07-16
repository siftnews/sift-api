package com.siftnews.source.adapter.out.persistence;

import com.siftnews.source.domain.Source;

final class SourceMapper {

    private SourceMapper() {
    }

    static Source toDomain(SourceJpaEntity entity) {
        return Source.restore(entity.getId(), entity.getName(), entity.getType(), entity.getUrl(),
                entity.getLang(), entity.getCategory(), entity.isActive(), entity.getLastCrawledAt());
    }
}
