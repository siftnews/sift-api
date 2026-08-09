package com.siftnews.delivery.application.port.in;

/** sendStep이 한 task를 처리한 결과를 배치 어댑터에 전달한다. */
public enum SendDeliveryTaskResult {

    SENT,
    CLAIM_SKIPPED,
    FAILED
}
