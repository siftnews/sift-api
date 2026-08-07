package com.siftnews.delivery.adapter.in.batch;

import com.siftnews.content.api.IssueCatalog;
import com.siftnews.delivery.application.port.out.SendEmailPort;
import com.siftnews.delivery.application.port.out.UpdateDeliveryTaskPort;
import com.siftnews.delivery.domain.DeliveryTask;
import com.siftnews.delivery.domain.DeliveryTaskStatus;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

final class DeliveryEmailWriter implements ItemWriter<DeliveryTask> {

    private final Long issueId;
    private final IssueCatalog issueCatalog;
    private final SendEmailPort sendEmailPort;
    private final UpdateDeliveryTaskPort updateDeliveryTaskPort;
    private final HtmlEmailRenderer renderer = new HtmlEmailRenderer();

    DeliveryEmailWriter(Long issueId, IssueCatalog issueCatalog, SendEmailPort sendEmailPort,
                        UpdateDeliveryTaskPort updateDeliveryTaskPort) {
        this.issueId = issueId;
        this.issueCatalog = issueCatalog;
        this.sendEmailPort = sendEmailPort;
        this.updateDeliveryTaskPort = updateDeliveryTaskPort;
    }

    @Override
    public void write(Chunk<? extends DeliveryTask> chunk) {
        var issue = issueCatalog.findNewsletterIssue(issueId)
                .orElseThrow(() -> new IllegalStateException("이슈를 찾을 수 없습니다: issueId=" + issueId));
        String html = renderer.render(issue);
        for (DeliveryTask task : chunk) {
            updateDeliveryTaskPort.updateStatus(task.getDeliveryTaskId(), DeliveryTaskStatus.SENDING, null);
            try {
                sendEmailPort.send(task.getEmail(), issue.title(), html);
                updateDeliveryTaskPort.updateStatus(task.getDeliveryTaskId(), DeliveryTaskStatus.SENT, null);
            } catch (RuntimeException exception) {
                updateDeliveryTaskPort.updateStatus(task.getDeliveryTaskId(), DeliveryTaskStatus.FAILED,
                        exception.getMessage());
            }
        }
    }
}
