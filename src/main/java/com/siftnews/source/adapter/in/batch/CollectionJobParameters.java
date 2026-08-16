package com.siftnews.source.adapter.in.batch;

/** collectionJob 파라미터 키 — 트리거와 Job이 같은 이름을 쓰도록 한곳에 모은다. */
final class CollectionJobParameters {

    /**
     * 재기동을 허용하기 위한 식별 파라미터.
     * <p>
     * 없으면 collectionJob은 파라미터가 빈 채로 기동돼 매번 같은 JobInstance가 되고, 두 번째
     * 주기부터 {@code JobInstanceAlreadyCompleteException}으로 거부된다. 수집은
     * {@code UNIQUE(normalized_url)} 중복 무시 저장이라 다시 돌아도 결과가 덧나지 않는다.
     */
    static final String LAUNCHED_AT = "launchedAt";

    /** loadtest fixture URL namespace를 실행별로 분리하는 식별자. */
    static final String RUN_ID = "runId";

    private CollectionJobParameters() {
    }
}
