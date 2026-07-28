package com.siftnews.content.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 한 클러스터를 대표할 기사를 고르는 규칙 — <b>가장 최근에 발행된 것</b>, 같으면 articleId가 작은 것.
 * <p>
 * {@link DedupClusterer}(클러스터를 만들 때)와 스코어링(클러스터당 한 건으로 줄일 때)이 같은
 * 규칙을 써야 한다. 두 곳에 따로 적으면 한쪽만 고쳐졌을 때 대표가 갈라지는데, 예외가 나지 않고
 * <b>다른 기사가 뉴스레터에 실릴 뿐</b>이라 발견이 늦다. 그래서 정의를 여기 한 곳에 둔다.
 * <p>
 * 동점 처리는 {@code articleId}가 작은 쪽이다 — 발행시각이 없는 피드(null)끼리 묶이면 전부
 * 동점이 되는데, 그때 순서가 흔들리면 같은 입력에 다른 결과가 나온다(재실행 멱등성).
 */
public final class RepresentativeRule {

    private static final Comparator<CandidateArticle> BY_PREFERENCE = Comparator
            .comparing((CandidateArticle a) -> a.publishedAt() == null ? Instant.MIN : a.publishedAt())
            .thenComparing(CandidateArticle::articleId, Comparator.reverseOrder());

    private RepresentativeRule() {
    }

    static CandidateArticle pick(Collection<CandidateArticle> members) {
        return members.stream().max(BY_PREFERENCE).orElseThrow();
    }

    /**
     * 클러스터마다 대표 한 건만 남긴다 — 같은 사건 기사가 한 뉴스레터에 중복 게재되지 않도록.
     * <p>
     * {@code dedupClusterId}가 없는 기사(컷 탈락·미클러스터)는 자기 자신이 곧 클러스터다.
     * 입력 순서를 유지해 같은 입력이 같은 출력을 내게 한다.
     */
    public static List<CandidateArticle> reduceToRepresentatives(List<CandidateArticle> articles) {
        Map<String, List<CandidateArticle>> byCluster = new LinkedHashMap<>();
        for (CandidateArticle article : articles) {
            String key = article.dedupClusterId() == null
                    ? "single-" + article.articleId()
                    : article.dedupClusterId();
            byCluster.computeIfAbsent(key, k -> new ArrayList<>()).add(article);
        }
        return byCluster.values().stream().map(RepresentativeRule::pick).toList();
    }
}
