package com.siftnews.content.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 윈도우 안의 클러스터 크기 분포 — 화제성 점수(trendScore)의 입력이다.
 * <p>
 * <b>토픽 필터를 적용하기 전의 후보 전체</b>로 만들어야 한다. 화제성은 "몇 개 매체가 이 사건을
 * 다뤘나"라서 토픽과 무관한 값인데, 필터 뒤의 집합으로 세면 토픽마다 같은 사건의 화제성이
 * 달라진다.
 * <p>
 * 집계 범위를 {@code loadCandidates(from, to)}가 돌려준 목록으로 한정하는 것이 D-032 불변식
 * (2)를 <b>구조적으로</b> 지키는 방법이다 — 별도 집계 쿼리를 쓰면 대상이 윈도우를 벗어날 수 있다.
 */
public final class ClusterWindow {

    private final Map<String, Integer> sizeByClusterId;
    private final int maxSize;

    private ClusterWindow(Map<String, Integer> sizeByClusterId) {
        this.sizeByClusterId = sizeByClusterId;
        this.maxSize = sizeByClusterId.values().stream().mapToInt(Integer::intValue).max().orElse(1);
    }

    public static ClusterWindow of(List<CandidateArticle> windowCandidates) {
        Map<String, Integer> sizes = new HashMap<>();
        for (CandidateArticle article : windowCandidates) {
            if (article.dedupClusterId() != null) {
                sizes.merge(article.dedupClusterId(), 1, Integer::sum);
            }
        }
        return new ClusterWindow(sizes);
    }

    /** 클러스터에 묶이지 않은 기사는 자기 혼자가 클러스터이므로 1이다. */
    public int sizeOf(CandidateArticle article) {
        if (article.dedupClusterId() == null) {
            return 1;
        }
        return sizeByClusterId.getOrDefault(article.dedupClusterId(), 1);
    }

    public int maxSize() {
        return maxSize;
    }
}
