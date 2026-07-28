package com.siftnews.source.adapter.out.persistence;

import com.siftnews.source.application.port.out.SaveArticlePort;
import com.siftnews.source.domain.Article;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class ArticlePersistenceAdapter implements SaveArticlePort {

    private final ArticleJpaRepository articleJpaRepository;

    /**
     * 이미 저장된 기사와 <b>입력 안에서 중복된 기사</b>를 모두 걸러 신규만 저장한다.
     * <p>
     * 입력 내 중복까지 거르는 이유: 한 번의 수집에 같은 기사가 여러 소스로 들어오는 일이 흔하다
     * (신디케이션·같은 피드의 중복 게재). DB 기존분만 걸러내면 {@code saveAll}이
     * UNIQUE(normalized_url) 위반을 내고, 배치에서는 그 소스 전체가 skip된다
     * (2026-07-26 e2e에서 실제로 발생).
     */
    @Override
    public int saveNew(List<Article> articles) {
        if (articles.isEmpty()) {
            return 0;
        }

        List<String> normalizedUrls = articles.stream().map(Article::getNormalizedUrl).toList();
        Set<String> seenNormalizedUrls = articleJpaRepository.findByNormalizedUrlIn(normalizedUrls).stream()
                .map(ArticleJpaEntity::getNormalizedUrl)
                .collect(Collectors.toCollection(HashSet::new));

        List<ArticleJpaEntity> newEntities = new ArrayList<>();
        for (Article article : articles) {
            // add가 false면 이미 DB에 있거나 이번 입력에서 앞서 나온 기사다
            if (seenNormalizedUrls.add(article.getNormalizedUrl())) {
                newEntities.add(ArticleMapper.toEntity(article));
            }
        }

        articleJpaRepository.saveAll(newEntities);
        return newEntities.size();
    }
}
