package com.siftnews.delivery.application.service;

import com.siftnews.content.api.IssueCatalog;
import com.siftnews.content.api.NewsletterArticle;
import com.siftnews.content.api.NewsletterIssue;
import com.siftnews.content.api.ScheduledIssueReference;
import com.siftnews.delivery.application.port.out.SendEmailPort;
import com.siftnews.delivery.application.port.out.UpdateDeliveryTaskPort;
import com.siftnews.delivery.domain.DeliveryException;
import com.siftnews.delivery.domain.DeliveryFailureCategory;
import com.siftnews.delivery.domain.DeliveryTask;
import com.siftnews.delivery.domain.DeliveryTaskStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SendDeliveryTaskServiceTest {

    private static final Long ISSUE_ID = 11L;

    @Test
    void claimsBeforeSendingAndMarksSentAfterSuccessfulDelivery() {
        RecordingTaskUpdates updates = new RecordingTaskUpdates();
        RecordingEmailSender sender = new RecordingEmailSender(false);
        SendDeliveryTaskService service = new SendDeliveryTaskService(issueCatalog(), sender, updates);

        service.send(task(101L, "reader@example.com"));

        assertThat(sender.sent).singleElement().satisfies(email -> {
            assertThat(email.recipient()).isEqualTo("reader@example.com");
            assertThat(email.subject()).isEqualTo("이번 주 Sift");
            assertThat(email.htmlBody()).contains("기사 제목");
        });
        assertThat(updates.operations).containsExactly(
                "claim:101", "sent:101");
    }

    @Test
    void skipsSendingWhenAnotherExecutionAlreadyClaimedTask() {
        RecordingTaskUpdates updates = new RecordingTaskUpdates();
        updates.claimResult = 0;
        RecordingEmailSender sender = new RecordingEmailSender(false);
        SendDeliveryTaskService service = new SendDeliveryTaskService(issueCatalog(), sender, updates);

        service.send(task(102L, "reader@example.com"));

        assertThat(sender.sent).isEmpty();
        assertThat(updates.operations).containsExactly("claim:102");
    }

    @Test
    void storesSanitizedDeliveryFailureAfterSendError() {
        RecordingTaskUpdates updates = new RecordingTaskUpdates();
        RecordingEmailSender sender = new RecordingEmailSender(true);
        SendDeliveryTaskService service = new SendDeliveryTaskService(issueCatalog(), sender, updates);

        service.send(task(103L, "private@example.com"));

        assertThat(updates.failedError).isEqualTo(
                "메일 발송 실패: category=SMTP, causeType=IllegalStateException");
        assertThat(updates.failedError).doesNotContain("private@example.com", "SMTP unavailable");
        assertThat(updates.operations).containsExactly("claim:103", "failed:103");
    }

    private static DeliveryTask task(Long taskId, String email) {
        return DeliveryTask.restore(taskId, 5L, ISSUE_ID, 7L, email, DeliveryTaskStatus.PENDING,
                "key-" + taskId);
    }

    private static IssueCatalog issueCatalog() {
        return new IssueCatalog() {
            @Override
            public List<ScheduledIssueReference> findScheduled(LocalDate runDate) {
                return List.of();
            }

            @Override
            public Optional<NewsletterIssue> findNewsletterIssue(Long issueId) {
                return Optional.of(new NewsletterIssue(issueId, "이번 주 Sift",
                        List.of(new NewsletterArticle(1, "기사 제목", "https://example.com/article"))));
            }
        };
    }

    private record SentEmail(
            String recipient,
            String subject,
            String htmlBody
    ) {
    }

    private static final class RecordingEmailSender implements SendEmailPort {

        private final boolean shouldFail;
        private final List<SentEmail> sent = new ArrayList<>();

        private RecordingEmailSender(boolean shouldFail) {
            this.shouldFail = shouldFail;
        }

        @Override
        public void send(String recipient, String subject, String htmlBody) {
            if (shouldFail) {
                throw new DeliveryException(DeliveryFailureCategory.SMTP,
                        new IllegalStateException("SMTP unavailable for private@example.com"));
            }
            sent.add(new SentEmail(recipient, subject, htmlBody));
        }
    }

    private static final class RecordingTaskUpdates implements UpdateDeliveryTaskPort {

        private int claimResult = 1;
        private final List<String> operations = new ArrayList<>();
        private String failedError;

        @Override
        public int claimPending(Long taskId) {
            operations.add("claim:" + taskId);
            return claimResult;
        }

        @Override
        public int markSent(Long taskId) {
            operations.add("sent:" + taskId);
            return 1;
        }

        @Override
        public int markFailed(Long taskId, String error) {
            operations.add("failed:" + taskId);
            failedError = error;
            return 1;
        }
    }
}
