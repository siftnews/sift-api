package com.siftnews.content.adapter.out.persistence;

import com.siftnews.content.application.port.out.LoadTopicPort;
import com.siftnews.content.domain.Topic;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class TopicPersistenceAdapter implements LoadTopicPort {

    private final TopicJpaRepository topicJpaRepository;

    @Override
    public Optional<Topic> load(Long topicId) {
        return topicJpaRepository.findById(topicId).map(TopicMapper::toDomain);
    }
}
