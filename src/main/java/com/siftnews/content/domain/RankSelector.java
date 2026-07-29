package com.siftnews.content.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rank &amp; Select 단계 (SELECTION §2.5) — 점수를 게재 목록으로 바꾼다.
 * <p>
 * 순서: {@code scoreThreshold} 컷(품질 하한) → 점수 내림차순 → <b>소스 쏠림 완화</b> →
 * 상위 {@code maxItems}건.
 * <p>
 * <b>전면 MMR을 넣지 않은 이유</b>: SELECTION §2.5의 다양성 요구는 "같은 소스/같은 클러스터
 * 쏠림 방지"인데, 같은 클러스터 쏠림은 이 단계에 <b>도달하지 않는다</b> — 스코어링이 이미
 * 클러스터당 대표 한 건으로 줄이기 때문이다({@link RepresentativeRule}). 남은 것은 소스
 * 쏠림뿐이라, 유사도 계산을 다시 도는 MMR 대신 소스별 감점으로 충분하다. 제목 유사도 기반
 * MMR은 실측으로 필요가 확인되면 그때 넣는다(SELECTION §6 열린 질문).
 */
public final class RankSelector {

    /**
     * 같은 소스에서 한 건 더 뽑을 때마다 곱하는 감점 계수.
     * <p>
     * 금지가 아니라 감점인 이유: 그날 그 토픽의 좋은 기사가 실제로 한 매체에 몰릴 수 있다.
     * 하드 상한을 두면 그런 날 빈자리를 낮은 점수로 채우게 되므로, <b>점수 차가 충분히 크면
     * 같은 소스라도 이기게</b> 둔다. 0.7이면 두 번째 건은 0.7배, 세 번째는 0.49배가 된다.
     */
    private static final double SAME_SOURCE_PENALTY = 0.7;

    private RankSelector() {
    }

    public static List<IssueItem> select(Topic topic, List<ArticleScore> scores) {
        List<ArticleScore> eligible = new ArrayList<>(scores.stream()
                .filter(score -> score.score() >= topic.getScoreThreshold())
                .toList());
        // 동점일 때 순서가 흔들리면 같은 입력에 다른 호가 나온다 — articleId로 고정한다.
        eligible.sort(Comparator.comparingDouble(ArticleScore::score).reversed()
                .thenComparing(ArticleScore::articleId));

        List<IssueItem> selected = new ArrayList<>();
        Map<Long, Integer> pickedPerSource = new HashMap<>();
        int limit = Math.min(topic.getMaxItems(), eligible.size());

        for (int rank = 1; rank <= limit; rank++) {
            ArticleScore best = pickBest(eligible, pickedPerSource);
            eligible.remove(best);
            pickedPerSource.merge(best.sourceId(), 1, Integer::sum);
            // rank·score는 뽑힌 순서와 원점수를 남긴다 — 감점은 선택에만 쓰고 기록하지 않는다.
            selected.add(new IssueItem(best.articleId(), rank, best.score()));
        }
        return List.copyOf(selected);
    }

    private static ArticleScore pickBest(List<ArticleScore> eligible, Map<Long, Integer> pickedPerSource) {
        ArticleScore best = null;
        double bestEffective = Double.NEGATIVE_INFINITY;
        for (ArticleScore candidate : eligible) {
            double effective = effectiveScore(candidate, pickedPerSource);
            // eligible이 이미 정렬돼 있으므로 동점이면 앞선 것(점수 높고 id 작은 쪽)이 유지된다.
            if (effective > bestEffective) {
                bestEffective = effective;
                best = candidate;
            }
        }
        return best;
    }

    private static double effectiveScore(ArticleScore score, Map<Long, Integer> pickedPerSource) {
        int alreadyPicked = pickedPerSource.getOrDefault(score.sourceId(), 0);
        return score.score() * Math.pow(SAME_SOURCE_PENALTY, alreadyPicked);
    }
}
