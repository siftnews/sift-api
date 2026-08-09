package com.siftnews.delivery.adapter.in.batch;

import com.siftnews.delivery.application.port.out.LoadDeliveryJobPort;
import com.siftnews.delivery.application.port.out.LoadPendingDeliveryTasksPort;
import com.siftnews.delivery.domain.DeliveryTask;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamReader;

import java.util.List;

final class PendingDeliveryTaskReader implements ItemStreamReader<DeliveryTask> {

    static final int PAGE_SIZE = 500;
    private static final String LAST_TASK_ID = "delivery.pending.lastTaskId";

    private final LoadPendingDeliveryTasksPort loadPendingDeliveryTasksPort;
    private final Long deliveryJobId;
    private final int pageSize;
    private List<DeliveryTask> page = List.of();
    private int pageIndex;
    private Long lastTaskId;

    PendingDeliveryTaskReader(Long issueId, LoadDeliveryJobPort loadDeliveryJobPort,
                              LoadPendingDeliveryTasksPort loadPendingDeliveryTasksPort) {
        this(issueId, loadDeliveryJobPort, loadPendingDeliveryTasksPort, PAGE_SIZE);
    }

    PendingDeliveryTaskReader(Long issueId, LoadDeliveryJobPort loadDeliveryJobPort,
                              LoadPendingDeliveryTasksPort loadPendingDeliveryTasksPort, int pageSize) {
        this.deliveryJobId = loadDeliveryJobPort.loadByIssueId(issueId)
                .orElseThrow(() -> new IllegalStateException("발송 작업을 찾을 수 없습니다: issueId=" + issueId))
                .getDeliveryJobId();
        if (pageSize <= 0) {
            throw new IllegalArgumentException("task 페이지 크기는 0보다 커야 합니다: " + pageSize);
        }
        this.loadPendingDeliveryTasksPort = loadPendingDeliveryTasksPort;
        this.pageSize = pageSize;
    }

    @Override
    public DeliveryTask read() {
        if (pageIndex >= page.size()) {
            page = loadPendingDeliveryTasksPort.loadPending(deliveryJobId, lastTaskId, pageSize);
            pageIndex = 0;
        }
        if (page.isEmpty()) {
            return null;
        }
        DeliveryTask task = page.get(pageIndex++);
        lastTaskId = task.getDeliveryTaskId();
        return task;
    }

    @Override
    public void open(ExecutionContext executionContext) {
        lastTaskId = executionContext.containsKey(LAST_TASK_ID)
                ? executionContext.getLong(LAST_TASK_ID) : null;
        page = List.of();
        pageIndex = 0;
    }

    @Override
    public void update(ExecutionContext executionContext) {
        if (lastTaskId == null) {
            executionContext.remove(LAST_TASK_ID);
        } else {
            executionContext.putLong(LAST_TASK_ID, lastTaskId);
        }
    }

    @Override
    public void close() {
        page = List.of();
        pageIndex = 0;
    }
}
