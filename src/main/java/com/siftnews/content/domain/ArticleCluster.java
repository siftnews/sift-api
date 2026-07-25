package com.siftnews.content.domain;

import java.util.List;

/**
 * dedup 클러스터 — 같은 사건을 보도한 기사 묶음.
 * <p>
 * {@code clusterId}는 최소 memberId에서 파생한 키로, 대표 선정과는
 * 별개 기준이다(D-031) — 신규 기사 합류로 대표({@code representativeId})가
 * 바뀌어도 멤버 구성이 유지되는 한 clusterId는 재실행에 안정적이다(최소
 * 멤버의 이탈·클러스터 병합 시는 예외). {@code memberIds}에는 대표를 포함한
 * 전체 구성원이 담기며, 크기는 화제성(trendScore) 입력이 된다(SELECTION §2).
 */
public record ArticleCluster(String clusterId, Long representativeId, List<Long> memberIds) {

    public int size() {
        return memberIds.size();
    }
}
