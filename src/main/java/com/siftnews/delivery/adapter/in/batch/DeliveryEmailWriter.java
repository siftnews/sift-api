package com.siftnews.delivery.adapter.in.batch;

import com.siftnews.delivery.application.port.in.SendDeliveryTaskUseCase;
import com.siftnews.delivery.application.port.in.SendDeliveryTaskResult;
import com.siftnews.delivery.domain.DeliveryTask;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

final class DeliveryEmailWriter implements ItemWriter<DeliveryTask> {

    static final String FAILED_TASK_COUNT = "delivery.failedTaskCount";

    private final SendDeliveryTaskUseCase sendDeliveryTaskUseCase;

    DeliveryEmailWriter(SendDeliveryTaskUseCase sendDeliveryTaskUseCase) {
        this.sendDeliveryTaskUseCase = sendDeliveryTaskUseCase;
    }

    @Override
    public void write(Chunk<? extends DeliveryTask> chunk) {
        int failedTaskCount = 0;
        for (DeliveryTask task : chunk) {
            if (sendDeliveryTaskUseCase.send(task) == SendDeliveryTaskResult.FAILED) {
                failedTaskCount++;
            }
        }
        if (failedTaskCount > 0 && StepSynchronizationManager.getContext() != null) {
            var executionContext = StepSynchronizationManager.getContext().getStepExecution()
                    .getExecutionContext();
            executionContext.putInt(FAILED_TASK_COUNT,
                    executionContext.getInt(FAILED_TASK_COUNT, 0) + failedTaskCount);
        }
    }
}
