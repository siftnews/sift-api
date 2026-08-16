package com.siftnews.source.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface SourceJpaRepository extends JpaRepository<SourceJpaEntity, Long> {

    List<SourceJpaEntity> findByActiveTrue();

    List<SourceJpaEntity> findByActiveTrueAndUrlStartingWith(String urlPrefix);

    Optional<SourceJpaEntity> findByIdAndActiveTrue(Long id);

    /**
     * url이 이미 있으면 아무것도 하지 않고 저장하며, 삽입된 행 수(0 또는 1)를 돌려준다.
     * <p>
     * "있는지 조회 → 없으면 save"는 두 인스턴스가 동시에 기동하면 <b>둘 다 없다고 판정</b>할 수 있어
     * 유니크 제약이 없으면 중복이 들어가고, 있으면 한쪽 기동이 예외로 죽는다. 확인과 삽입을 DB의
     * 한 문장으로 합쳐 그 창을 없앤다.
     * <p>
     * 네이티브 SQL인 이유: {@code ON CONFLICT DO NOTHING}은 JPQL에 없다. 대신 컬럼 목록을 손으로
     * 들고 있어야 해서 {@link SourceJpaEntity} 스키마가 바뀌면 여기도 같이 고쳐야 한다 — 시더는
     * Liquibase 마이그레이션으로 옮겨갈 예정이라 그때 함께 사라진다(MVP-DESIGN §2).
     * 감사 컬럼도 JPA Auditing을 우회하므로 호출자가 {@code now}를 넘긴다.
     */
    @Modifying
    @Query(value = """
            INSERT INTO source (name, type, url, lang, category, active, last_crawled_at, created_at, updated_at)
            VALUES (:name, :type, :url, :lang, :category, :active, NULL, :now, :now)
            ON CONFLICT (url) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("name") String name,
                       @Param("type") String type,
                       @Param("url") String url,
                       @Param("lang") String lang,
                       @Param("category") String category,
                       @Param("active") boolean active,
                       @Param("now") Instant now);
}
