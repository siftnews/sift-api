/**
 * 공통 모듈 — 모든 바운디드 컨텍스트가 공유하는 기반 요소.
 * <p>
 * 다른 모듈이 상속·참조하는 공개 타입({@code BaseEntity}, {@code BusinessException})은
 * 이 루트 패키지에 두어 Modulith 상 공개 API로 노출한다.
 * config/auditing 등 세부 구성은 internal 서브패키지에 둔다.
 * common 은 어떤 비즈니스 모듈에도 의존하지 않는다(단방향 공용).
 */
@ApplicationModule(
        displayName = "Common",
        allowedDependencies = {}
)
package com.siftnews.common;

import org.springframework.modulith.ApplicationModule;
