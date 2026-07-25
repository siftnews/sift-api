package com.siftnews.content.application.port.out;

import java.util.Map;

/**
 * dedup 클러스터 id 벌크 갱신 — article은 Source 소유(D-018)이므로 실 구현은
 * Source named interface의 갱신 오퍼레이션을 호출한다(D-030). 배선은 M2-5.
 * <p>
 * 실행 단위로 로드한 후보 전체에 대한 계산 결과를 한 번에 덮어쓴다(D-031). 값이
 * {@code null}이면 해당 기사의 clusterId를 해제한다 — 컷 탈락 등으로 더 이상
 * 클러스터에 속하지 않게 된 기사가 이전 실행의 clusterId를 잔존시키지 않도록 한다.
 * {@code clusterIdsByArticleId}가 비어 있으면 아무 것도 하지 않는다(no-op) — 호출자가
 * 빈 맵으로도 이 포트를 부를 수 있다.
 * <p>
 * null 값을 담아야 하므로 호출자는 {@code Map.of}·{@code Map.copyOf}·
 * {@code Collectors.toMap}·{@code ConcurrentHashMap} 등 null 값을 거부하는 Map 구현을
 * 쓸 수 없다. 이 맵은 호출마다 새로 만들어 넘기며 호출 이후 더 이상 참조하지 않으므로,
 * 구현체가 그대로 들고 있어도 된다.
 */
public interface UpdateArticleClusterPort {

    void updateClusters(Map<Long, String> clusterIdsByArticleId);
}
