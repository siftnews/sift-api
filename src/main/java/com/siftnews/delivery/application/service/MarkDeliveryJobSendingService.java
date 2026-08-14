package com.siftnews.delivery.application.service;

import com.siftnews.delivery.application.port.in.MarkDeliveryJobSendingUseCase;
import com.siftnews.delivery.application.port.out.LoadDeliveryJobPort;
import com.siftnews.delivery.application.port.out.UpdateDeliveryJobPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class MarkDeliveryJobSendingService implements MarkDeliveryJobSendingUseCase {

    private final LoadDeliveryJobPort loadDeliveryJobPort;
    private final UpdateDeliveryJobPort updateDeliveryJobPort;

    @Override
    public void markSending(Long issueId) {
        Long deliveryJobId = loadDeliveryJobPort.loadByIssueId(issueId)
                .orElseThrow(() -> new IllegalStateException("발송 작업을 찾을 수 없습니다: issueId=" + issueId))
                .getDeliveryJobId();
        updateDeliveryJobPort.markSending(deliveryJobId);
    }
}
