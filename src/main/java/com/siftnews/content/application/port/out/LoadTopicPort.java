package com.siftnews.content.application.port.out;

import com.siftnews.content.domain.Topic;

import java.util.List;
import java.util.Optional;

public interface LoadTopicPort {

    Optional<Topic> load(Long topicId);

    /** 발행 대상 토픽 — 전 토픽 DAILY 고정이므로 활성 여부만 본다 (D-019). */
    List<Topic> loadActive();
}
