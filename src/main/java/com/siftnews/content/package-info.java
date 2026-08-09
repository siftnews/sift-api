/**
 * 콘텐츠(선별) 모듈 — 기사 저장, 토픽별 스코어링·랭킹, 템플릿 이슈 구성.
 * 상세 파이프라인은 https://github.com/siftnews/sift-docs/blob/main/references/SELECTION.md 참고.
 * <p>
 * 다른 모듈과는 도메인 이벤트로 통신한다(동기 조회가 꼭 필요하면 port.in 을
 * named interface 로만 노출).
 * <p>
 * {@code source :: article-catalog} 의존은 D-018·D-030의 귀결이다 — article 애그리거트는
 * Source 소유인데 선별이 후보를 읽고 dedup 클러스터를 갱신해야 한다. Source가 공개한
 * named interface({@code com.siftnews.source.api})만 보이며, 그 밖의 Source internal은
 * {@code ApplicationModules.verify()}가 차단한다.
 */
@ApplicationModule(
        displayName = "Content",
        allowedDependencies = {"common", "source :: article-catalog"}
)
package com.siftnews.content;

import org.springframework.modulith.ApplicationModule;
