/**
 * 발송 모듈 — DeliveryTask 스냅샷, 대량 발송, 재시도. 성능 개선 서사의 핵심.
 * 발송 수단은 SendEmailPort(아웃바운드 포트) 뒤로 추상화(로컬 SMTP ↔ SES).
 * <p>
 * 다른 모듈과는 도메인 이벤트로 통신한다. 코드 의존이 생기면 allowedDependencies 를 확장한다.
 */
@ApplicationModule(
        displayName = "Delivery",
        allowedDependencies = {"common"}
)
package com.siftnews.delivery;

import org.springframework.modulith.ApplicationModule;
