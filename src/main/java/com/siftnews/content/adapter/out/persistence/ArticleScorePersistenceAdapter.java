package com.siftnews.content.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siftnews.content.application.port.out.SaveArticleScorePort;
import com.siftnews.content.domain.ArticleScore;
import com.siftnews.content.domain.ContentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
class ArticleScorePersistenceAdapter implements SaveArticleScorePort {

    private final ArticleScoreJpaRepository articleScoreJpaRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    @Transactional
    public void saveAll(List<ArticleScore> scores) {
        Instant now = clock.instant();
        for (ArticleScore score : scores) {
            articleScoreJpaRepository.upsert(score.articleId(), score.topicId(), score.score(),
                    serialize(score), score.computedAt(), now);
        }
    }

    private String serialize(ArticleScore score) {
        try {
            return objectMapper.writeValueAsString(score.breakdown());
        } catch (JsonProcessingException e) {
            // 근거를 잃은 점수는 튜닝·설명에 쓸 수 없으므로 조용히 넘기지 않는다.
            throw new ContentException("점수 근거를 JSON으로 변환하지 못했습니다: articleId=" + score.articleId(), e);
        }
    }
}
