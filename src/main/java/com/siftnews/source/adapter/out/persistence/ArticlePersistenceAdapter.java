package com.siftnews.source.adapter.out.persistence;

import com.siftnews.source.application.port.out.SaveArticlePort;
import com.siftnews.source.domain.Article;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class ArticlePersistenceAdapter implements SaveArticlePort {

    private final ArticleJpaRepository articleJpaRepository;

    @Override
    public int saveNew(List<Article> articles) {
        if (articles.isEmpty()) {
            return 0;
        }

        List<String> normalizedUrls = articles.stream().map(Article::getNormalizedUrl).toList();
        Set<String> existingNormalizedUrls = articleJpaRepository.findByNormalizedUrlIn(normalizedUrls).stream()
                .map(ArticleJpaEntity::getNormalizedUrl)
                .collect(Collectors.toSet());

        List<ArticleJpaEntity> newEntities = articles.stream()
                .filter(article -> !existingNormalizedUrls.contains(article.getNormalizedUrl()))
                .map(ArticleMapper::toEntity)
                .toList();

        articleJpaRepository.saveAll(newEntities);
        return newEntities.size();
    }
}
