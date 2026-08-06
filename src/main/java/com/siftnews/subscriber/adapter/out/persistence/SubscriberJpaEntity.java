package com.siftnews.subscriber.adapter.out.persistence;

import com.siftnews.common.BaseEntity;
import com.siftnews.subscriber.domain.SubscriberStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "subscriber")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class SubscriberJpaEntity extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriberStatus status;

    @Column(name = "preferred_send_hour", nullable = false)
    private int preferredSendHour;

    SubscriberJpaEntity(String email, SubscriberStatus status, int preferredSendHour) {
        this.email = email;
        this.status = status;
        this.preferredSendHour = preferredSendHour;
    }
}
