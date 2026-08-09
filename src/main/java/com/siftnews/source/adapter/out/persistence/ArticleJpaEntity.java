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

/**
 * 기사 영속 엔티티.
 * <p>
 * 문자열 컬럼 길이를 명시하는 이유: JPA 기본값 {@code varchar(255)}로는 실제 피드를 감당하지
 * 못한다 — 2026-07-26 e2e에서 9개 소스 중 5개가 {@code value too long}으로 통째로 실패했다.
 * 본문은 길이 상한을 걸 근거가 없어 {@code text}로 둔다.
 */
@Entity
@Table(name = "article")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class ArticleJpaEntity extends BaseEntity {

    /** 긴 쿼리스트링(추적 파라미터 등)을 붙이는 피드가 있어 넉넉히 잡는다. */
    private static final int URL_MAX_LENGTH = 2048;

    /** 제목은 255를 넘는 경우가 드물게 있다(부제 포함 등). */
    private static final int TITLE_MAX_LENGTH = 1024;

    @Column(nullable = false)
    private Long sourceId;

    @Column(nullable = false, length = URL_MAX_LENGTH)
    private String url;

    @Column(nullable = false, unique = true, length = URL_MAX_LENGTH)
    private String normalizedUrl;

    @Column(length = TITLE_MAX_LENGTH)
    private String title;

    @Column(columnDefinition = "text")
    private String body;

    @Column(length = 16)
    private String lang;

    private Instant publishedAt;

    @Enumerated(EnumType.STRING)
    private Category category;

    /**
     * 선별(Content)이 매긴 dedup 클러스터 id — <b>Source는 값의 의미를 해석하지 않고 보관만 한다</b>.
     * <p>
     * article 스키마의 주인이 Source라 컬럼과 갱신 경로는 Source가 통제하지만(D-018·D-030),
     * 클러스터 소속은 Content의 판단이다. 그래서 도메인 {@code Article}에는 두지 않는다 —
     * 애그리거트가 이 값으로 아무 결정도 하지 않아, 넣으면 생성 시 항상 null인 빈 필드가 된다.
     * 갱신은 named interface를 통한 벌크 UPDATE로만 이뤄진다(D-031 — null은 클러스터 해제).
     */
    @Column(length = 64)
    private String dedupClusterId;

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
