package com.siftnews.delivery.adapter.in.batch;

import com.siftnews.delivery.application.port.out.LoadDeliveryJobPort;
import com.siftnews.delivery.application.port.out.LoadPendingDeliveryTasksPort;
import com.siftnews.delivery.domain.DeliveryTask;
import org.springframework.batch.item.ItemReader;

import java.util.Iterator;

final class PendingDeliveryTaskReader implements ItemReader<DeliveryTask> {

    private final Iterator<DeliveryTask> tasks;

    PendingDeliveryTaskReader(Long issueId, LoadDeliveryJobPort loadDeliveryJobPort,
                              LoadPendingDeliveryTasksPort loadPendingDeliveryTasksPort) {
        Long jobId = loadDeliveryJobPort.loadByIssueId(issueId)
                .orElseThrow(() -> new IllegalStateException("발송 작업을 찾을 수 없습니다: issueId=" + issueId))
                .getDeliveryJobId();
        this.tasks = loadPendingDeliveryTasksPort.loadPending(jobId).iterator();
    }

    @Override
    public DeliveryTask read() {
        return tasks.hasNext() ? tasks.next() : null;
    }
}
