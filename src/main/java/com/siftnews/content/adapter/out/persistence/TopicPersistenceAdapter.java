package com.siftnews.content.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siftnews.content.application.port.out.LoadTopicPort;
import com.siftnews.content.application.port.out.SaveTopicPort;
import com.siftnews.content.domain.ContentException;
import com.siftnews.content.domain.Topic;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class TopicPersistenceAdapter implements LoadTopicPort, SaveTopicPort {

    private final TopicJpaRepository topicJpaRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    public Optional<Topic> load(Long topicId) {
        return topicJpaRepository.findById(topicId).map(TopicMapper::toDomain);
    }

    @Override
    public List<Topic> loadActive() {
        return topicJpaRepository.findByActiveTrue().stream().map(TopicMapper::toDomain).toList();
    }

    /**
     * JSON 컬럼 4종은 네이티브 insert가 {@code @JdbcTypeCode(SqlTypes.JSON)} 매핑을 타지 않으므로
     * 여기서 직렬화해 넘긴다 — 리포지토리가 {@code jsonb}로 캐스팅한다.
     */
    @Override
    @Transactional
    public int saveNew(List<Topic> topics) {
        Instant now = clock.instant();
        int inserted = 0;
        for (Topic topic : topics) {
            inserted += topicJpaRepository.insertIfAbsent(topic.getName(), topic.getSlug(), topic.getLangScope(),
                    toJson(topic.getIncludeKeywords()), toJson(topic.getExcludeKeywords()),
                    toJson(topic.getKeywordWeights()), toJson(topic.getSourceCategories()),
                    topic.getRecencyHalfLifeHours(), topic.getMaxItems(), topic.getScoreThreshold(),
                    topic.isActive(), now);
        }
        return inserted;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new ContentException("토픽 JSON 컬럼 직렬화에 실패했습니다: " + value, e);
        }
    }
}
