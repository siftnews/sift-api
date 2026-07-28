package com.siftnews.source.application.port.out;

import com.siftnews.source.domain.Source;

import java.util.List;

public interface SaveSourcePort {

    /**
     * 아직 없는 소스만 저장하고 <b>실제로 새로 들어간 건수</b>를 돌려준다 — url이 이미 있으면 건너뛴다.
     * 여러 인스턴스가 동시에 호출해도 중복 없이, 예외 없이 끝나야 한다(재실행 멱등).
     */
    int saveNew(List<Source> sources);
}
