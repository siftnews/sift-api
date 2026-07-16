package com.siftnews.source.adapter.out.persistence;

import com.siftnews.source.domain.Article;

final class ArticleMapper {

    private ArticleMapper() {
    }

    static ArticleJpaEntity toEntity(Article article) {
        return new ArticleJpaEntity(article.getSourceId(), article.getUrl(), article.getNormalizedUrl(),
                article.getTitle(), article.getBody(), article.getLang(), article.getPublishedAt(),
                article.getCategory());
    }
}
