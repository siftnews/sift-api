package com.siftnews.content.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

interface TopicJpaRepository extends JpaRepository<TopicJpaEntity, Long> {

    List<TopicJpaEntity> findByActiveTrue();

    boolean existsBySlug(String slug);

    /**
     * slug가 이미 있으면 아무것도 하지 않고 저장하며, 삽입된 행 수(0 또는 1)를 돌려준다.
     * <p>
     * "있는지 조회 → 없으면 save"는 두 인스턴스가 동시에 기동하면 <b>둘 다 없다고 판정</b>한다.
     * {@code topic.slug}에 UNIQUE가 있어 중복이 들어가지는 않지만, 대신 한쪽 기동이 예외로 죽는다.
     * 확인과 삽입을 DB의 한 문장으로 합쳐 그 창을 없앤다 — {@code SourceJpaRepository}와 같은 방식.
     * <p>
     * JSON 컬럼 4종은 {@code @JdbcTypeCode(SqlTypes.JSON)} 매핑을 우회하므로 호출자가 직렬화한
     * 문자열을 넘기고 여기서 {@code jsonb}로 캐스팅한다. 네이티브 SQL이라 컬럼 목록을 손으로 들고
     * 있어야 해서 {@link TopicJpaEntity} 스키마가 바뀌면 여기도 같이 고쳐야 한다 — 시더는
     * Liquibase 마이그레이션으로 옮겨갈 예정이라 그때 함께 사라진다.
     * 감사 컬럼도 JPA Auditing을 우회하므로 호출자가 {@code now}를 넘긴다.
     */
    @Modifying
    @Query(value = """
            INSERT INTO topic (name, slug, lang_scope, include_keywords, exclude_keywords,
                               keyword_weights, source_categories, recency_half_life_hours,
                               max_items, score_threshold, active, created_at, updated_at)
            VALUES (:name, :slug, :langScope,
                    CAST(:includeKeywords AS jsonb), CAST(:excludeKeywords AS jsonb),
                    CAST(:keywordWeights AS jsonb), CAST(:sourceCategories AS jsonb),
                    :recencyHalfLifeHours, :maxItems, :scoreThreshold, :active, :now, :now)
            ON CONFLICT (slug) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("name") String name,
                       @Param("slug") String slug,
                       @Param("langScope") String langScope,
                       @Param("includeKeywords") String includeKeywords,
                       @Param("excludeKeywords") String excludeKeywords,
                       @Param("keywordWeights") String keywordWeights,
                       @Param("sourceCategories") String sourceCategories,
                       @Param("recencyHalfLifeHours") int recencyHalfLifeHours,
                       @Param("maxItems") int maxItems,
                       @Param("scoreThreshold") double scoreThreshold,
                       @Param("active") boolean active,
                       @Param("now") Instant now);
}
