package com.siftnews.source.adapter.out.persistence;

import com.siftnews.common.BaseEntity;
import com.siftnews.source.domain.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "article")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class ArticleJpaEntity extends BaseEntity {

    @Column(nullable = false)
    private Long sourceId;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false, unique = true)
    private String normalizedUrl;

    private String title;

    private String body;

    private String lang;

    private Instant publishedAt;

    @Enumerated(EnumType.STRING)
    private Category category;

    ArticleJpaEntity(Long sourceId, String url, String normalizedUrl, String title, String body,
                      String lang, Instant publishedAt, Category category) {
        this.sourceId = sourceId;
        this.url = url;
        this.normalizedUrl = normalizedUrl;
        this.title = title;
        this.body = body;
        this.lang = lang;
        this.publishedAt = publishedAt;
        this.category = category;
    }
}
