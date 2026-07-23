package com.siftnews.content.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * dedup 클러스터링 (SELECTION §2.2, D-030) — 같은 사건 기사를 한 클러스터로 묶는다.
 * <p>
 * 병합 기준: ① {@code normalizedUrl} 완전 일치 ② 제목 Jaccard ≥ 임계값.
 * union-find로 전이적 병합(A~B, B~C면 A~C 한 클러스터)한다. 대표는 최신 발행분,
 * 동률이면 작은 articleId. MVP는 O(n²) 비교 — 후보가 소규모라 충분.
 * <p>
 * 실제 교차 소스 dedup은 ②(제목 Jaccard)가 담당한다. 저장 시점에
 * {@code UNIQUE(normalized_url)}로 같은 정규화 URL은 한 행만 남으므로(D-018),
 * 후보 집합에서 ①이 서로 다른 두 기사를 병합하는 일은 사실상 없다 — ①은
 * 순수 도메인 함수로서의 방어적 가드다(입력이 같은 URL이면 병합).
 */
public final class DedupClusterer {

    private DedupClusterer() {
    }

    public static List<ArticleCluster> cluster(List<CandidateArticle> articles, double jaccardThreshold) {
        int n = articles.size();
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                CandidateArticle a = articles.get(i);
                CandidateArticle b = articles.get(j);
                if (sameUrl(a, b) || TitleSimilarity.jaccard(a.title(), b.title()) >= jaccardThreshold) {
                    union(parent, i, j);
                }
            }
        }
        Map<Integer, List<Integer>> groups = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            groups.computeIfAbsent(find(parent, i), k -> new ArrayList<>()).add(i);
        }
        List<ArticleCluster> clusters = new ArrayList<>();
        for (List<Integer> indexes : groups.values()) {
            CandidateArticle representative = representative(articles, indexes);
            List<Long> memberIds = indexes.stream().map(idx -> articles.get(idx).articleId()).toList();
            clusters.add(new ArticleCluster("c-" + representative.articleId(), representative.articleId(), memberIds));
        }
        return clusters;
    }

    private static boolean sameUrl(CandidateArticle a, CandidateArticle b) {
        return a.normalizedUrl() != null && a.normalizedUrl().equals(b.normalizedUrl());
    }

    private static CandidateArticle representative(List<CandidateArticle> articles, List<Integer> indexes) {
        return indexes.stream()
                .map(articles::get)
                .max(Comparator
                        .comparing((CandidateArticle a) -> a.publishedAt() == null ? Instant.MIN : a.publishedAt())
                        .thenComparing(CandidateArticle::articleId, Comparator.reverseOrder()))
                .orElseThrow();
    }

    private static int find(int[] parent, int x) {
        while (parent[x] != x) {
            parent[x] = parent[parent[x]];
            x = parent[x];
        }
        return x;
    }

    private static void union(int[] parent, int a, int b) {
        parent[find(parent, a)] = find(parent, b);
    }
}
