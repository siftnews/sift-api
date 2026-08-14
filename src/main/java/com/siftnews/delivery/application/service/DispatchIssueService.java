package com.siftnews.delivery.application.service;

import com.siftnews.delivery.application.port.in.DispatchIssueUseCase;
import com.siftnews.delivery.application.port.in.DispatchIssueSummary;
import com.siftnews.delivery.application.port.out.CountDeliveryTasksPort;
import com.siftnews.delivery.application.port.out.LoadDeliveryJobPort;
import com.siftnews.delivery.application.port.out.SaveDeliveryJobPort;
import com.siftnews.delivery.application.port.out.SaveDeliveryTaskPort;
import com.siftnews.delivery.application.port.out.UpdateDeliveryJobPort;
import com.siftnews.delivery.domain.DeliveryJob;
import com.siftnews.delivery.domain.DeliveryTask;
import com.siftnews.subscriber.api.SubscriberCatalog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DispatchIssueService implements DispatchIssueUseCase {

    private final LoadDeliveryJobPort loadDeliveryJobPort;
    private final SaveDeliveryJobPort saveDeliveryJobPort;
    private final SaveDeliveryTaskPort saveDeliveryTaskPort;
    private final CountDeliveryTasksPort countDeliveryTasksPort;
    private final UpdateDeliveryJobPort updateDeliveryJobPort;
    private final SubscriberCatalog subscriberCatalog;

    @Override
    @Transactional
    public DispatchIssueSummary dispatch(Long issueId, Long topicId, int preferredSendHour) {
        DeliveryJob deliveryJob = loadDeliveryJobPort.loadByIssueId(issueId)
                .orElseGet(() -> saveDeliveryJobPort.save(DeliveryJob.create(issueId)));
        var tasks = subscriberCatalog.findActiveRecipients(topicId, preferredSendHour).stream()
                .map(recipient -> DeliveryTask.pending(deliveryJob.getDeliveryJobId(), issueId,
                        recipient.subscriberId(), recipient.email()))
                .toList();
        int createdTaskCount = saveDeliveryTaskPort.saveIfAbsent(tasks);
        int totalTaskCount = countDeliveryTasksPort.countByDeliveryJobId(deliveryJob.getDeliveryJobId());
        if (updateDeliveryJobPort.updateTotalCount(deliveryJob.getDeliveryJobId(), totalTaskCount) != 1) {
            throw new IllegalStateException("발송 작업 집계 갱신에 실패했습니다: jobId="
                    + deliveryJob.getDeliveryJobId());
        }
        log.info("[measure] delivery snapshot createdTasks={}", createdTaskCount);
        return new DispatchIssueSummary(deliveryJob.getDeliveryJobId(), createdTaskCount);
    }
}
