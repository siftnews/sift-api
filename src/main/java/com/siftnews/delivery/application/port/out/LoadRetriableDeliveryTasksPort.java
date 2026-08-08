package com.siftnews.delivery.application.port.out;

import com.siftnews.delivery.domain.DeliveryTask;

import java.time.Instant;
import java.util.List;

public interface LoadRetriableDeliveryTasksPort {

    /**
     * 도래한 FAILED task를 id 오름차순으로 한 페이지 조회한다.
     * {@code afterTaskId}가 null이면 첫 페이지이며, 최대 시도 횟수를 넘은 task는 제외한다.
     */
    List<DeliveryTask> loadRetriable(Instant now, Long afterTaskId, int maxAttempts, int limit);
}
