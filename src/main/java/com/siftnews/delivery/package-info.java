/**
 * 발송 모듈 — DeliveryTask 스냅샷, 대량 발송, 재시도. 성능 개선 서사의 핵심.
 * 발송 수단은 SendEmailPort(아웃바운드 포트) 뒤로 추상화(로컬 SMTP ↔ SES).
 * <p>
 * 다른 모듈과는 도메인 이벤트로 통신한다. 발송 시점에는 Content가 공개한 topic-catalog를 통해
 * 렌더링 데이터를 동기 조회한다.
 */
@ApplicationModule(
        displayName = "Delivery",
        allowedDependencies = {"common", "content :: topic-catalog", "subscriber :: subscriber-catalog"}
)
package com.siftnews.delivery;

import org.springframework.modulith.ApplicationModule;
