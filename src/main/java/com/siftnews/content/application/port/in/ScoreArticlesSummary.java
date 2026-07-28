package com.siftnews.content.application.port.in;

/**
 * 스코어링 1회 실행 결과.
 *
 * @param loaded    윈도우에서 로드한 후보 수
 * @param filtered  토픽 필터를 통과한 수
 * @param scored    클러스터당 대표 한 건으로 줄인 뒤 실제로 점수를 매겨 저장한 수
 */
public record ScoreArticlesSummary(int loaded, int filtered, int scored) {
}
