package com.siftnews.delivery.application.service;

import com.siftnews.content.api.IssueCatalog;
import com.siftnews.delivery.application.port.in.SendDeliveryTaskUseCase;
import com.siftnews.delivery.application.port.out.SendEmailPort;
import com.siftnews.delivery.application.port.out.UpdateDeliveryTaskPort;
import com.siftnews.delivery.domain.DeliveryException;
import com.siftnews.delivery.domain.DeliveryTask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SendDeliveryTaskService implements SendDeliveryTaskUseCase {

    private final IssueCatalog issueCatalog;
    private final SendEmailPort sendEmailPort;
    private final UpdateDeliveryTaskPort updateDeliveryTaskPort;
    private final HtmlEmailRenderer renderer = new HtmlEmailRenderer();

    @Override
    public void send(DeliveryTask task) {
        if (updateDeliveryTaskPort.claimPending(task.getDeliveryTaskId()) != 1) {
            return;
        }

        try {
            var issue = issueCatalog.findNewsletterIssue(task.getIssueId())
                    .orElseThrow(() -> new IllegalStateException("이슈를 찾을 수 없습니다: issueId="
                            + task.getIssueId()));
            sendEmailPort.send(task.getEmail(), issue.title(), renderer.render(issue));
        } catch (RuntimeException exception) {
            markFailed(task, exception);
            return;
        }

        if (updateDeliveryTaskPort.markSent(task.getDeliveryTaskId()) != 1) {
            throw new IllegalStateException("SENT 상태 전이에 실패했습니다: taskId=" + task.getDeliveryTaskId());
        }
    }

    private void markFailed(DeliveryTask task, RuntimeException exception) {
        String error = DeliveryException.safeMessage(exception);
        if (updateDeliveryTaskPort.markFailed(task.getDeliveryTaskId(), error) != 1) {
            throw new IllegalStateException("FAILED 상태 전이에 실패했습니다: taskId="
                    + task.getDeliveryTaskId());
        }
    }
}
