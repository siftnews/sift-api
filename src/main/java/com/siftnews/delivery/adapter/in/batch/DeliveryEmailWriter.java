package com.siftnews.delivery.adapter.in.batch;

import com.siftnews.delivery.application.port.in.SendDeliveryTaskUseCase;
import com.siftnews.delivery.domain.DeliveryTask;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

final class DeliveryEmailWriter implements ItemWriter<DeliveryTask> {

    private final SendDeliveryTaskUseCase sendDeliveryTaskUseCase;

    DeliveryEmailWriter(SendDeliveryTaskUseCase sendDeliveryTaskUseCase) {
        this.sendDeliveryTaskUseCase = sendDeliveryTaskUseCase;
    }

    @Override
    public void write(Chunk<? extends DeliveryTask> chunk) {
        for (DeliveryTask task : chunk) {
            sendDeliveryTaskUseCase.send(task);
        }
    }
}
