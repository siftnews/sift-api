package com.siftnews.content.api;

import java.util.Optional;

public interface TopicCatalog {

    Optional<TopicReference> findActive(Long topicId);

    Optional<TopicReference> findActiveBySlug(String slug);
}
