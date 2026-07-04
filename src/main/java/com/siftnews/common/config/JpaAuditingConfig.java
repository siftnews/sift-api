package com.siftnews.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 활성화 — {@code @CreatedDate}/{@code @LastModifiedDate}
 * ({@link com.siftnews.common.BaseEntity})를 자동으로 채운다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
