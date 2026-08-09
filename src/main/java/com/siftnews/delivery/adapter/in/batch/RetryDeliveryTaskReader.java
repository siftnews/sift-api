package com.siftnews.delivery.adapter.in.batch;

import com.siftnews.delivery.application.port.out.LoadRetriableDeliveryTasksPort;
import com.siftnews.delivery.domain.DeliveryTask;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamReader;

import java.time.Clock;
import java.util.List;

final class RetryDeliveryTaskReader implements ItemStreamReader<DeliveryTask> {

    static final int PAGE_SIZE = 500;
    private static final String LAST_TASK_ID = "delivery.retry.lastTaskId";

    private final LoadRetriableDeliveryTasksPort loadRetriableDeliveryTasksPort;
    private final Clock clock;
    private final int maxAttempts;
    private final int pageSize;
    private List<DeliveryTask> page = List.of();
    private int pageIndex;
    private Long lastTaskId;

    RetryDeliveryTaskReader(LoadRetriableDeliveryTasksPort loadRetriableDeliveryTasksPort,
                            Clock clock, int maxAttempts) {
        this(loadRetriableDeliveryTasksPort, clock, maxAttempts, PAGE_SIZE);
    }

    RetryDeliveryTaskReader(LoadRetriableDeliveryTasksPort loadRetriableDeliveryTasksPort,
                            Clock clock, int maxAttempts, int pageSize) {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("최대 발송 시도 횟수는 0보다 커야 합니다: " + maxAttempts);
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("task 페이지 크기는 0보다 커야 합니다: " + pageSize);
        }
        this.loadRetriableDeliveryTasksPort = loadRetriableDeliveryTasksPort;
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.pageSize = pageSize;
    }

    @Override
    public DeliveryTask read() {
        if (pageIndex >= page.size()) {
            page = loadRetriableDeliveryTasksPort.loadRetriable(clock.instant(), lastTaskId,
                    maxAttempts, pageSize);
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
