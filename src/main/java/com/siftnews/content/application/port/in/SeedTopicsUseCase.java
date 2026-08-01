package com.siftnews.content.application.port.in;

import com.siftnews.content.domain.Topic;

import java.util.List;

public interface SeedTopicsUseCase {

    /**
     * 선별 대상 토픽 카탈로그를 심고 새로 들어간 건수를 돌려준다.
     * <p>
     * <b>어떤 토픽을 심을지는 호출자(인바운드 어댑터)가 정한다</b> — 애플리케이션은 카탈로그의
     * 출처(코드 상수·설정 파일·관리 API 중 무엇이든)를 알지 않는다. Source 쪽
     * {@code SeedSourcesUseCase}와 같은 계약이다.
     */
    int seed(List<Topic> topics);
}
