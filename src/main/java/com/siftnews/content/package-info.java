/**
 * 콘텐츠(선별) 모듈 — 기사 저장, 토픽별 스코어링·랭킹, 템플릿 이슈 구성.
 * 상세 파이프라인은 docs/SELECTION.md 참고.
 * <p>
 * 다른 모듈과는 도메인 이벤트로 통신한다(동기 조회가 꼭 필요하면 port.in 을
 * named interface 로만 노출). 코드 의존이 생기면 allowedDependencies 를 확장한다.
 */
@ApplicationModule(
        displayName = "Content",
        allowedDependencies = {"common"}
)
package com.siftnews.content;

import org.springframework.modulith.ApplicationModule;
