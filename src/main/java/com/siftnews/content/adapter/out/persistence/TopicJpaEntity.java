package com.siftnews.content.adapter.out.persistence;

import com.siftnews.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

/**
 * topic 테이블 매핑 — 도메인 {@link com.siftnews.content.domain.Topic}과 분리된 영속 전용 엔티티(D-009).
 * <p>
 * 키워드 목록·가중치·소스 카테고리는 별도 테이블 대신 JSON 컬럼으로 저장한다
 * ({@code @JdbcTypeCode(SqlTypes.JSON)}) — MVP 스키마(MVP-DESIGN §2)의 keyword_weights[json] 관례.
 */
@Entity
@Table(name = "topic")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class TopicJpaEntity extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String langScope;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> includeKeywords;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> excludeKeywords;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Double> keywordWeights;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> sourceCategories;

    @Column(nullable = false)
    private int recencyHalfLifeHours;

    @Column(nullable = false)
    private int maxItems;

    @Column(nullable = false)
    private double scoreThreshold;

    @Column(nullable = false)
    private boolean active;

    TopicJpaEntity(String name, String slug, String langScope,
                   List<String> includeKeywords, List<String> excludeKeywords,
                   Map<String, Double> keywordWeights, List<String> sourceCategories,
                   int recencyHalfLifeHours, int maxItems, double scoreThreshold, boolean active) {
        this.name = name;
        this.slug = slug;
        this.langScope = langScope;
        this.includeKeywords = includeKeywords;
        this.excludeKeywords = excludeKeywords;
        this.keywordWeights = keywordWeights;
        this.sourceCategories = sourceCategories;
        this.recencyHalfLifeHours = recencyHalfLifeHours;
        this.maxItems = maxItems;
        this.scoreThreshold = scoreThreshold;
        this.active = active;
    }
}
