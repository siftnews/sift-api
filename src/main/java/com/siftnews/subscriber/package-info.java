/**
 * 구독 모듈 — 구독/해지, 토픽 구독, 수신 동의, 바운스·옵트아웃.
 * <p>
 * 다른 모듈과는 도메인 이벤트로 통신한다. 코드 의존이 생기면 allowedDependencies 를 확장한다.
 */
@ApplicationModule(
        displayName = "Subscriber",
        allowedDependencies = {"common"}
)
package com.siftnews.subscriber;

import org.springframework.modulith.ApplicationModule;
