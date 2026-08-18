package com.siftnews.delivery.application.service;

import com.siftnews.content.api.IssueCatalog;
import com.siftnews.delivery.application.port.in.SendDeliveryTaskResult;
import com.siftnews.delivery.application.port.in.SendDeliveryTaskUseCase;
import com.siftnews.delivery.application.port.out.SendEmailPort;
import com.siftnews.delivery.application.port.out.UpdateDeliveryTaskPort;
import com.siftnews.delivery.domain.DeliveryException;
import com.siftnews.delivery.domain.DeliveryTask;
import com.siftnews.delivery.domain.DeliveryTaskStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class SendDeliveryTaskService implements SendDeliveryTaskUseCase {

    private final IssueCatalog issueCatalog;
    private final SendEmailPort sendEmailPort;
    private final UpdateDeliveryTaskPort updateDeliveryTaskPort;
    private final Clock clock;
    private final DeliveryRetryPolicy retryPolicy;
    private final HtmlEmailRenderer renderer;

    @Autowired
    public SendDeliveryTaskService(IssueCatalog issueCatalog,
                                   SendEmailPort sendEmailPort,
                                   UpdateDeliveryTaskPort updateDeliveryTaskPort,
                                   Clock clock,
                                   DeliveryRetryPolicy retryPolicy) {
        this(issueCatalog, sendEmailPort, updateDeliveryTaskPort, clock, retryPolicy,
                new HtmlEmailRenderer());
    }

    SendDeliveryTaskService(IssueCatalog issueCatalog,
                            SendEmailPort sendEmailPort,
                            UpdateDeliveryTaskPort updateDeliveryTaskPort,
                            Clock clock,
                            DeliveryRetryPolicy retryPolicy,
                            HtmlEmailRenderer renderer) {
        this.issueCatalog = issueCatalog;
        this.sendEmailPort = sendEmailPort;
        this.updateDeliveryTaskPort = updateDeliveryTaskPort;
        this.clock = clock;
        this.retryPolicy = retryPolicy;
        this.renderer = renderer;
    }

    @Override
    public SendDeliveryTaskResult send(DeliveryTask task) {
        if (!claim(task)) {
            return SendDeliveryTaskResult.CLAIM_SKIPPED;
        }

        try {
            var issue = issueCatalog.findNewsletterIssue(task.getIssueId())
                    .orElseThrow(() -> new IllegalStateException("이슈를 찾을 수 없습니다: issueId="
                            + task.getIssueId()));
            sendEmailPort.send(task.getEmail(), issue.title(), renderer.render(issue));
        } catch (RuntimeException exception) {
            recordFailure(task, exception);
            return SendDeliveryTaskResult.FAILED;
        }

        if (updateDeliveryTaskPort.markSent(task.getDeliveryTaskId()) != 1) {
            throw new IllegalStateException("SENT 상태 전이에 실패했습니다: taskId=" + task.getDeliveryTaskId());
        }
        return SendDeliveryTaskResult.SENT;
    }

    private boolean claim(DeliveryTask task) {
        if (task.getStatus() == DeliveryTaskStatus.PENDING) {
            return updateDeliveryTaskPort.claimPending(task.getDeliveryTaskId()) == 1;
        }
        if (task.getStatus() == DeliveryTaskStatus.FAILED) {
            return updateDeliveryTaskPort.claimFailed(task.getDeliveryTaskId(), clock.instant(),
                    retryPolicy.maxAttempts()) == 1;
        }
        return false;
    }

    private void recordFailure(DeliveryTask task, RuntimeException exception) {
        String error = DeliveryException.safeMessage(exception);
        int nextAttemptCount = task.getAttemptCount() + 1;
        if (retryPolicy.isRetryable(exception) && nextAttemptCount < retryPolicy.maxAttempts()) {
            Instant nextRetryAt = retryPolicy.nextRetryAt(clock.instant(), nextAttemptCount);
            if (updateDeliveryTaskPort.markFailed(task.getDeliveryTaskId(), error, nextAttemptCount,
                    nextRetryAt) != 1) {
                throw new IllegalStateException("FAILED 상태 전이에 실패했습니다: taskId="
                        + task.getDeliveryTaskId());
            }
            return;
        }
        if (updateDeliveryTaskPort.markDead(task.getDeliveryTaskId(), error, nextAttemptCount) != 1) {
            throw new IllegalStateException("DEAD 상태 전이에 실패했습니다: taskId="
                    + task.getDeliveryTaskId());
        }
    }
}
