package com.siftnews.delivery.application.port.out;

import com.siftnews.delivery.domain.DeliveryTask;

import java.util.List;

public interface LoadPendingDeliveryTasksPort {

    /**
     * {@code id > afterTaskId} 조건의 PENDING task를 id 오름차순으로 한 페이지 조회한다.
     * {@code afterTaskId}가 null이면 첫 페이지이며, 반환 목록의 최대 크기는 {@code limit}이다.
     */
    List<DeliveryTask> loadPending(Long deliveryJobId, Long afterTaskId, int limit);
}
