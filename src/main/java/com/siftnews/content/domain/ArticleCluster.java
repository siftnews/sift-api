package com.siftnews.content.domain;

import java.util.List;

/**
 * dedup 클러스터 — 같은 사건을 보도한 기사 묶음.
 * <p>
 * {@code clusterId}는 대표 기사 id에서 파생한 안정적 키다. {@code memberIds}에는
 * 대표를 포함한 전체 구성원이 담기며, 크기는 화제성(trendScore) 입력이 된다(SELECTION §2).
 */
public record ArticleCluster(String clusterId, Long representativeId, List<Long> memberIds) {

    public int size() {
        return memberIds.size();
    }
}
