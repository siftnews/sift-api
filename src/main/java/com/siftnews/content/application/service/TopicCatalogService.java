package com.siftnews.content.application.service;

import com.siftnews.content.api.TopicCatalog;
import com.siftnews.content.api.TopicReference;
import com.siftnews.content.application.port.out.LoadTopicPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
class TopicCatalogService implements TopicCatalog {
    private final LoadTopicPort loadTopicPort;

    @Override
    public Optional<TopicReference> findActive(Long topicId) {
        return loadTopicPort.load(topicId)
                .filter(topic -> topic.isActive())
                .map(topic -> new TopicReference(topic.getTopicId()));
    }
}
