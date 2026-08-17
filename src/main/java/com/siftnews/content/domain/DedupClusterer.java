package com.siftnews.content.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * dedup 클러스터링 (SELECTION §2.2, D-030) — 같은 사건 기사를 한 클러스터로 묶는다.
 * <p>
 * 병합 기준: ① {@code normalizedUrl} 완전 일치 ② 제목 Jaccard ≥ 임계값.
 * union-find로 전이적 병합(A~B, B~C면 A~C 한 클러스터)한다. 대표는 최신 발행분,
 * 동률이면 작은 articleId. 제목 token set은 한 번만 계산하고, 동일한 비어 있지 않은
 * token signature는 먼저 묶은 뒤 signature 간 비교를 수행한다. 따라서 Jaccard 의미는
 * 유지하면서 결정적 fixture처럼 같은 제목이 반복되는 입력의 비교 수를 줄인다.
 * <p>
 * {@code clusterId}는 대표 선정과 분리된 별도 기준으로 발급한다(D-031) —
 * 최소 memberId에서 파생. 신규 기사는 id가 단조 증가하는 한 기존보다 큰
 * articleId를 받으므로, 기존 클러스터에 합류해도(대표가 바뀌더라도) 멤버
 * 구성이 유지되는 한 clusterId는 재실행에 안정적이다. 다만 최소 멤버가
 * 윈도우 밖으로 이탈하거나 정규화 컷에 탈락해 후보에서 빠지면 다음 최소
 * id로 바뀌며, 두 클러스터가 신규 기사로 연결되어 병합되는 경우도 클러스터
 * 정체성 자체가 달라진 것이라 clusterId 변경을 수용한다.
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

        List<Set<String>> titleTokens = articles.stream()
                .map(article -> TitleSimilarity.tokenize(article.title()))
                .toList();

        Map<String, List<Integer>> articlesByUrl = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            String normalizedUrl = articles.get(i).normalizedUrl();
            if (normalizedUrl != null) {
                articlesByUrl.computeIfAbsent(normalizedUrl, ignored -> new ArrayList<>()).add(i);
            }
        }
        unionGroups(parent, articlesByUrl.values());

        Map<Set<String>, List<Integer>> articlesByTitleTokens = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            Set<String> tokens = titleTokens.get(i);
            // 빈 제목은 기존 Jaccard 계약상 두 빈 제목도 0.0이므로 그룹화하지 않는다.
            if (!tokens.isEmpty()) {
                articlesByTitleTokens.computeIfAbsent(tokens, ignored -> new ArrayList<>()).add(i);
            }
        }
        if (jaccardThreshold <= 0.0) {
            unionAll(parent);
        } else if (jaccardThreshold <= 1.0) {
            unionGroups(parent, articlesByTitleTokens.values());

            List<Map.Entry<Set<String>, List<Integer>>> titleGroups = new ArrayList<>(articlesByTitleTokens.entrySet());
            for (int i = 0; i < titleGroups.size(); i++) {
                Set<String> leftTokens = titleGroups.get(i).getKey();
                int leftIndex = titleGroups.get(i).getValue().get(0);
                for (int j = i + 1; j < titleGroups.size(); j++) {
                    Set<String> rightTokens = titleGroups.get(j).getKey();
                    if (TitleSimilarity.jaccard(leftTokens, rightTokens) >= jaccardThreshold) {
                        union(parent, leftIndex, titleGroups.get(j).getValue().get(0));
                    }
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
            long minMemberId = memberIds.stream().mapToLong(Long::longValue).min().orElseThrow();
            clusters.add(new ArticleCluster("c-" + minMemberId, representative.articleId(), memberIds));
        }
        return clusters;
    }

    private static void unionGroups(int[] parent, Iterable<List<Integer>> groups) {
        for (List<Integer> indexes : groups) {
            int first = indexes.get(0);
            for (int i = 1; i < indexes.size(); i++) {
                union(parent, first, indexes.get(i));
            }
        }
    }

    private static void unionAll(int[] parent) {
        for (int i = 1; i < parent.length; i++) {
            union(parent, 0, i);
        }
    }

    /** 규칙 본체는 {@link RepresentativeRule} — 스코어링이 클러스터를 한 건으로 줄일 때도 같은 것을 쓴다. */
    private static CandidateArticle representative(List<CandidateArticle> articles, List<Integer> indexes) {
        return RepresentativeRule.pick(indexes.stream().map(articles::get).toList());
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
