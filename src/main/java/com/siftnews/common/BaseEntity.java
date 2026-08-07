package com.siftnews.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * 모든 JPA 엔티티의 공통 상위 타입 (식별자 + 감사 컬럼).
 * <p>
 * 도메인 POJO 와 분리된 <b>영속 계층 전용</b> 기반 클래스다(D-009).
 * 생성/수정 시각은 JPA Auditing({@link JpaAuditingConfig})이 자동으로 채운다.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    protected BaseEntity() {
    }

    protected BaseEntity(Long id) {
        this.id = id;
    }

    // NOTE: MVP 는 IDENTITY. 대량 발송(delivery_task) 배치 insert 성능 단계에서
    //       SEQUENCE + allocationSize 전환을 검토한다(성능 로드맵 V2~).
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
}
