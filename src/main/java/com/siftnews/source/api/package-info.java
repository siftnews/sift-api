/**
 * Source가 다른 모듈에 <b>의도적으로 공개</b>하는 표면 (Spring Modulith named interface).
 * <p>
 * article 애그리거트는 Source 소유이지만(D-018) 선별(Content)이 후보를 읽고 dedup 클러스터를
 * 갱신해야 한다. 모듈 internal을 직접 참조하면 경계가 무너지므로, 필요한 오퍼레이션만 이 패키지에
 * 노출하고 Content는 이것만 본다(D-030). 여기 없는 타입은 Content에서 보이지 않는다.
 */
@NamedInterface("article-catalog")
package com.siftnews.source.api;

import org.springframework.modulith.NamedInterface;
