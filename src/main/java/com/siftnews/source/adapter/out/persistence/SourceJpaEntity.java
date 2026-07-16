package com.siftnews.source.adapter.out.persistence;

import com.siftnews.common.BaseEntity;
import com.siftnews.source.domain.Category;
import com.siftnews.source.domain.SourceType;
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
@Table(name = "source")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class SourceJpaEntity extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceType type;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String lang;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(nullable = false)
    private boolean active;

    private Instant lastCrawledAt;

    SourceJpaEntity(String name, SourceType type, String url, String lang, Category category,
                     boolean active, Instant lastCrawledAt) {
        this.name = name;
        this.type = type;
        this.url = url;
        this.lang = lang;
        this.category = category;
        this.active = active;
        this.lastCrawledAt = lastCrawledAt;
    }

    void updateLastCrawledAt(Instant at) {
        this.lastCrawledAt = at;
    }
}
