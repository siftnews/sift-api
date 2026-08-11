package com.siftnews.content.application.service;

import com.siftnews.content.api.TopicCatalog;
import com.siftnews.content.api.TopicReference;
import com.siftnews.content.application.port.out.LoadTopicPort;
import com.siftnews.content.application.port.out.LoadTopicBySlugPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
class TopicCatalogService implements TopicCatalog {
    private final LoadTopicPort loadTopicPort;
    private final LoadTopicBySlugPort loadTopicBySlugPort;

    @Override
    public Optional<TopicReference> findActive(Long topicId) {
        return loadTopicPort.load(topicId)
                .filter(topic -> topic.isActive())
                .map(topic -> new TopicReference(topic.getTopicId()));
    }

    @Override
    public Optional<TopicReference> findActiveBySlug(String slug) {
        return loadTopicBySlugPort.loadBySlug(slug)
                .filter(topic -> topic.isActive())
                .map(topic -> new TopicReference(topic.getTopicId()));
    }
}
