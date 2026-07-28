package com.siftnews.content.application.port.out;

import com.siftnews.content.domain.Topic;

import java.util.Optional;

public interface LoadTopicPort {

    Optional<Topic> load(Long topicId);
}
