package com.siftnews.content.adapter.out.persistence;

import com.siftnews.common.BaseEntity;
import com.siftnews.content.domain.IssueStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * issue 테이블 매핑 (MVP-DESIGN §2).
 * <p>
 * ERD 초안에 없던 {@code run_date}를 두고 {@code UNIQUE(topic_id, run_date)}를 건다 —
 * 같은 날 같은 토픽을 다시 돌렸을 때 호가 두 개 생기면 구독자에게 같은 날 두 통이 나갈 수 있다.
 * {@code scheduled_at}만으로는 "몇 일자 호인가"를 표현하지 못해 이 판정을 할 수 없다.
 */
@Entity
@Table(name = "issue",
        uniqueConstraints = @UniqueConstraint(name = "uk_issue_topic_run_date",
                columnNames = {"topic_id", "run_date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class IssueJpaEntity extends BaseEntity {

    @Column(nullable = false)
    private Long topicId;

    @Column(nullable = false)
    private LocalDate runDate;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueStatus status;

    private Instant scheduledAt;

    private Instant publishedAt;
}
