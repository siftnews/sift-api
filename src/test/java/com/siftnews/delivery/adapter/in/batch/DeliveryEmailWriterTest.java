package com.siftnews.delivery.adapter.in.batch;

import com.siftnews.content.api.IssueCatalog;
import com.siftnews.content.api.NewsletterArticle;
import com.siftnews.content.api.NewsletterIssue;
import com.siftnews.content.api.ScheduledIssueReference;
import com.siftnews.delivery.application.port.out.SendEmailPort;
import com.siftnews.delivery.application.port.out.UpdateDeliveryTaskPort;
import com.siftnews.delivery.domain.DeliveryTask;
import com.siftnews.delivery.domain.DeliveryTaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.Chunk;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryEmailWriterTest {

    private static final Long ISSUE_ID = 11L;

    @Test
    void marksTaskSendingThenSentAfterSuccessfulDelivery() throws Exception {
        RecordingTaskUpdates updates = new RecordingTaskUpdates();
        RecordingEmailSender sender = new RecordingEmailSender(false);
        DeliveryEmailWriter writer = new DeliveryEmailWriter(ISSUE_ID, issueCatalog(), sender, updates);

        writer.write(new Chunk<>(List.of(task(101L, "reader@example.com"))));

        assertThat(sender.sent).singleElement().satisfies(email -> {
            assertThat(email.recipient()).isEqualTo("reader@example.com");
            assertThat(email.subject()).isEqualTo("이번 주 Sift");
            assertThat(email.htmlBody()).contains("기사 제목");
        });
        assertThat(updates.statuses).containsExactly(
                new StatusUpdate(101L, DeliveryTaskStatus.SENDING, null),
                new StatusUpdate(101L, DeliveryTaskStatus.SENT, null));
    }

    @Test
    void marksTaskFailedWhenEmailDeliveryThrows() throws Exception {
        RecordingTaskUpdates updates = new RecordingTaskUpdates();
        DeliveryEmailWriter writer = new DeliveryEmailWriter(ISSUE_ID, issueCatalog(),
                new RecordingEmailSender(true), updates);

        writer.write(new Chunk<>(List.of(task(102L, "reader@example.com"))));

        assertThat(updates.statuses).containsExactly(
                new StatusUpdate(102L, DeliveryTaskStatus.SENDING, null),
                new StatusUpdate(102L, DeliveryTaskStatus.FAILED, "SMTP unavailable"));
    }

    private static DeliveryTask task(Long taskId, String email) {
        return DeliveryTask.restore(taskId, 5L, 7L, email, DeliveryTaskStatus.PENDING, "key");
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

    private record StatusUpdate(
            Long taskId,
            DeliveryTaskStatus status,
            String error
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
                throw new IllegalStateException("SMTP unavailable");
            }
            sent.add(new SentEmail(recipient, subject, htmlBody));
        }
    }

    private static final class RecordingTaskUpdates implements UpdateDeliveryTaskPort {

        private final List<StatusUpdate> statuses = new ArrayList<>();

        @Override
        public void updateStatus(Long taskId, DeliveryTaskStatus status, String error) {
            statuses.add(new StatusUpdate(taskId, status, error));
        }
    }
}
