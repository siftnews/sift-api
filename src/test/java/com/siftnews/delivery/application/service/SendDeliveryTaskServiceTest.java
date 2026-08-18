package com.siftnews.delivery.application.service;

import com.siftnews.content.api.IssueCatalog;
import com.siftnews.content.api.NewsletterArticle;
import com.siftnews.content.api.NewsletterIssue;
import com.siftnews.content.api.ScheduledIssueReference;
import com.siftnews.delivery.application.port.in.SendDeliveryTaskResult;
import com.siftnews.delivery.application.port.out.SendEmailPort;
import com.siftnews.delivery.application.port.out.UpdateDeliveryTaskPort;
import com.siftnews.delivery.domain.DeliveryException;
import com.siftnews.delivery.domain.DeliveryFailureCategory;
import com.siftnews.delivery.domain.DeliveryTask;
import com.siftnews.delivery.domain.DeliveryTaskStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SendDeliveryTaskServiceTest {

    private static final Long ISSUE_ID = 11L;
    private static final Instant NOW = Instant.parse("2026-08-08T03:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final DeliveryRetryPolicy POLICY = new DeliveryRetryPolicy(
            3, Duration.ofMinutes(1), Duration.ofHours(1));

    @Test
    void claimsPendingBeforeSendingAndMarksSentAfterSuccessfulDelivery() {
        RecordingTaskUpdates updates = new RecordingTaskUpdates();
        RecordingEmailSender sender = new RecordingEmailSender(null);
        SendDeliveryTaskService service = service(sender, updates);

        assertThat(service.send(task(101L, "reader@example.com")))
                .isEqualTo(SendDeliveryTaskResult.SENT);

        assertThat(sender.sent).singleElement().satisfies(email -> {
            assertThat(email.recipient()).isEqualTo("reader@example.com");
            assertThat(email.subject()).isEqualTo("이번 주 Sift");
            assertThat(email.htmlBody()).contains("기사 제목");
        });
        assertThat(updates.operations).containsExactly("claim-pending:101", "sent:101");
    }

    @Test
    void skipsSendingWhenAnotherExecutionAlreadyClaimedPendingTask() {
        RecordingTaskUpdates updates = new RecordingTaskUpdates();
        updates.claimPendingResult = 0;
        RecordingEmailSender sender = new RecordingEmailSender(null);
        SendDeliveryTaskService service = service(sender, updates);

        assertThat(service.send(task(102L, "reader@example.com")))
                .isEqualTo(SendDeliveryTaskResult.CLAIM_SKIPPED);

        assertThat(sender.sent).isEmpty();
        assertThat(updates.operations).containsExactly("claim-pending:102");
    }

    @Test
    void recordsTransientFailureWithNextAttemptAndExponentialBackoff() {
        RecordingTaskUpdates updates = new RecordingTaskUpdates();
        RecordingEmailSender sender = new RecordingEmailSender(DeliveryFailureCategory.TRANSIENT);
        SendDeliveryTaskService service = service(sender, updates);

        assertThat(service.send(task(103L, "private@example.com")))
                .isEqualTo(SendDeliveryTaskResult.FAILED);

        assertThat(updates.failedError).isEqualTo(
                "메일 발송 실패: category=TRANSIENT, causeType=IllegalStateException");
        assertThat(updates.failedError).doesNotContain("private@example.com", "SMTP unavailable");
        assertThat(updates.failedAttemptCount).isEqualTo(1);
        assertThat(updates.failedNextRetryAt).isEqualTo(NOW.plusSeconds(60));
        assertThat(updates.operations).containsExactly("claim-pending:103", "failed:103");
    }

    @Test
    void sendsDueFailedTaskAndMarksItSent() {
        RecordingTaskUpdates updates = new RecordingTaskUpdates();
        RecordingEmailSender sender = new RecordingEmailSender(null);
        SendDeliveryTaskService service = service(sender, updates);

        service.send(retryTask(104L, 1));

        assertThat(sender.sent).hasSize(1);
        assertThat(updates.operations).containsExactly("claim-failed:104", "sent:104");
    }

    @Test
    void repeatsIssueLookupAndHtmlRenderingForEachTask() {
        RecordingTaskUpdates updates = new RecordingTaskUpdates();
        RecordingEmailSender sender = new RecordingEmailSender(null);
        RecordingIssueCatalog issueCatalog = new RecordingIssueCatalog();
        RecordingHtmlEmailRenderer renderer = new RecordingHtmlEmailRenderer();
        SendDeliveryTaskService service = service(issueCatalog, sender, updates, renderer);

        service.send(task(107L, "reader1@example.com"));
        service.send(task(108L, "reader2@example.com"));

        assertThat(issueCatalog.lookedUpIssueIds).containsExactly(ISSUE_ID, ISSUE_ID);
        assertThat(sender.sent).hasSize(2);
        assertThat(renderer.renderedIssueIds).containsExactly(ISSUE_ID, ISSUE_ID);
        assertThat(sender.sent.get(0).htmlBody()).isEqualTo(sender.sent.get(1).htmlBody());
    }

    @Test
    void movesPermanentFailureToDeadWithoutSchedulingRetry() {
        RecordingTaskUpdates updates = new RecordingTaskUpdates();
        RecordingEmailSender sender = new RecordingEmailSender(DeliveryFailureCategory.PERMANENT);
        SendDeliveryTaskService service = service(sender, updates);

        service.send(task(105L, "invalid@example.com"));

        assertThat(updates.deadError).isEqualTo(
                "메일 발송 실패: category=PERMANENT, causeType=IllegalStateException");
        assertThat(updates.deadAttemptCount).isEqualTo(1);
        assertThat(updates.operations).containsExactly("claim-pending:105", "dead:105");
        assertThat(updates.failedNextRetryAt).isNull();
    }

    @Test
    void movesRetryableFailureToDeadAfterMaximumAttempt() {
        RecordingTaskUpdates updates = new RecordingTaskUpdates();
        RecordingEmailSender sender = new RecordingEmailSender(DeliveryFailureCategory.TRANSIENT);
        SendDeliveryTaskService service = service(sender, updates);

        service.send(retryTask(106L, 2));

        assertThat(updates.operations).containsExactly("claim-failed:106", "dead:106");
        assertThat(updates.deadError).contains("category=TRANSIENT");
        assertThat(updates.deadAttemptCount).isEqualTo(3);
    }

    private static SendDeliveryTaskService service(RecordingEmailSender sender,
                                                    RecordingTaskUpdates updates) {
        return service(new RecordingIssueCatalog(), sender, updates);
    }

    private static SendDeliveryTaskService service(IssueCatalog issueCatalog,
                                                    RecordingEmailSender sender,
                                                    RecordingTaskUpdates updates) {
        return service(issueCatalog, sender, updates, new HtmlEmailRenderer());
    }

    private static SendDeliveryTaskService service(IssueCatalog issueCatalog,
                                                    RecordingEmailSender sender,
                                                    RecordingTaskUpdates updates,
                                                    HtmlEmailRenderer renderer) {
        return new SendDeliveryTaskService(issueCatalog, sender, updates, CLOCK, POLICY, renderer);
    }

    private static DeliveryTask task(Long taskId, String email) {
        return DeliveryTask.restore(taskId, 5L, ISSUE_ID, 7L, email, DeliveryTaskStatus.PENDING,
                0, null, null, "key-" + taskId);
    }

    private static DeliveryTask retryTask(Long taskId, int attemptCount) {
        return DeliveryTask.restore(taskId, 5L, ISSUE_ID, 7L, "reader@example.com", DeliveryTaskStatus.FAILED,
                attemptCount, NOW.minusSeconds(1), "previous failure", "key-" + taskId);
    }

    private static final class RecordingIssueCatalog implements IssueCatalog {

        private final List<Long> lookedUpIssueIds = new ArrayList<>();

        @Override
        public List<ScheduledIssueReference> findScheduled(LocalDate runDate) {
            return List.of();
        }

        @Override
        public Optional<NewsletterIssue> findNewsletterIssue(Long issueId) {
            lookedUpIssueIds.add(issueId);
            return Optional.of(new NewsletterIssue(issueId, "이번 주 Sift",
                    List.of(new NewsletterArticle(1, "기사 제목", "https://example.com/article"))));
        }
    }

    private static final class RecordingHtmlEmailRenderer extends HtmlEmailRenderer {

        private final List<Long> renderedIssueIds = new ArrayList<>();

        @Override
        String render(NewsletterIssue issue) {
            renderedIssueIds.add(issue.issueId());
            return super.render(issue);
        }
    }

    private record SentEmail(
            String recipient,
            String subject,
            String htmlBody
    ) {
    }

    private static final class RecordingEmailSender implements SendEmailPort {

        private final DeliveryFailureCategory failureCategory;
        private final List<SentEmail> sent = new ArrayList<>();

        private RecordingEmailSender(DeliveryFailureCategory failureCategory) {
            this.failureCategory = failureCategory;
        }

        @Override
        public void send(String recipient, String subject, String htmlBody) {
            if (failureCategory != null) {
                throw new DeliveryException(failureCategory,
                        new IllegalStateException("SMTP unavailable for private@example.com"));
            }
            sent.add(new SentEmail(recipient, subject, htmlBody));
        }
    }

    private static final class RecordingTaskUpdates implements UpdateDeliveryTaskPort {

        private int claimPendingResult = 1;
        private int claimFailedResult = 1;
        private final List<String> operations = new ArrayList<>();
        private String failedError;
        private int failedAttemptCount;
        private Instant failedNextRetryAt;
        private String deadError;
        private int deadAttemptCount;

        @Override
        public int claimPending(Long taskId) {
            operations.add("claim-pending:" + taskId);
            return claimPendingResult;
        }

        @Override
        public int claimFailed(Long taskId, Instant now, int maxAttempts) {
            operations.add("claim-failed:" + taskId);
            return claimFailedResult;
        }

        @Override
        public int markSent(Long taskId) {
            operations.add("sent:" + taskId);
            return 1;
        }

        @Override
        public int markFailed(Long taskId, String error, int attemptCount, Instant nextRetryAt) {
            operations.add("failed:" + taskId);
            failedError = error;
            failedAttemptCount = attemptCount;
            failedNextRetryAt = nextRetryAt;
            return 1;
        }

        @Override
        public int markDead(Long taskId, String error, int attemptCount) {
            operations.add("dead:" + taskId);
            deadError = error;
            deadAttemptCount = attemptCount;
            return 1;
        }
    }
}
