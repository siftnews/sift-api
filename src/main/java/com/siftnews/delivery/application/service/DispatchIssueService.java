package com.siftnews.delivery.application.service;

import com.siftnews.delivery.application.port.in.DispatchIssueUseCase;
import com.siftnews.delivery.application.port.in.DispatchIssueSummary;
import com.siftnews.delivery.application.port.out.LoadDeliveryJobPort;
import com.siftnews.delivery.application.port.out.SaveDeliveryJobPort;
import com.siftnews.delivery.application.port.out.SaveDeliveryTaskPort;
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
        log.info("[measure] delivery snapshot createdTasks={}", createdTaskCount);
        return new DispatchIssueSummary(deliveryJob.getDeliveryJobId(), createdTaskCount);
    }
}
