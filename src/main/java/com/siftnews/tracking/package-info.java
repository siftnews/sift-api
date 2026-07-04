/**
 * 추적 모듈 — 오픈/클릭/바운스 이벤트 수집·집계 (2차 범위).
 * <p>
 * 다른 모듈과는 도메인 이벤트로 통신한다. 코드 의존이 생기면 allowedDependencies 를 확장한다.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Tracking",
        allowedDependencies = {"common"}
)
package com.siftnews.tracking;
