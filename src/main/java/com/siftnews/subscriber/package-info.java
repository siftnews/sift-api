/**
 * 구독 모듈 — 구독/해지, 토픽 구독, 수신 동의, 바운스·옵트아웃.
 * <p>토픽 존재 확인은 Content의 공개 named interface만 사용한다.</p>
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Subscriber",
        allowedDependencies = {"common", "content :: topic-catalog"}
)
package com.siftnews.subscriber;
