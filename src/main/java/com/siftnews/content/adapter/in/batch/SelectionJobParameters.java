package com.siftnews.content.adapter.in.batch;

/** selectionJob 파라미터 키 — 트리거와 Step이 같은 이름을 쓰도록 한곳에 모은다. */
final class SelectionJobParameters {

    static final String TOPIC_ID = "topicId";
    static final String RUN_DATE = "runDate";
    static final String WINDOW_FROM = "from";
    static final String WINDOW_TO = "to";
    /** 재실행을 허용하기 위한 식별 파라미터 — 없으면 같은 날 재기동이 이미 완료된 인스턴스로 거부된다. */
    static final String LAUNCHED_AT = "launchedAt";

    private SelectionJobParameters() {
    }
}
