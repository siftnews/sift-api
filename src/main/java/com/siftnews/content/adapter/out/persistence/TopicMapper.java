package com.siftnews.content.adapter.out.persistence;

import com.siftnews.content.domain.Topic;

final class TopicMapper {

    private TopicMapper() {
    }

    static Topic toDomain(TopicJpaEntity entity) {
        return Topic.restore(entity.getId(), entity.getName(), entity.getSlug(), entity.getLangScope(),
                entity.getIncludeKeywords(), entity.getExcludeKeywords(), entity.getKeywordWeights(),
                entity.getSourceCategories(), entity.getRecencyHalfLifeHours(), entity.getMaxItems(),
                entity.getScoreThreshold(), entity.isActive());
    }
}
