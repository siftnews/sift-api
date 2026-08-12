package com.siftnews.delivery.adapter.in.batch;

import com.siftnews.content.api.IssueCatalog;
import com.siftnews.content.api.ScheduledIssueReference;
import com.siftnews.delivery.application.port.in.DispatchJobRunSummary;
import com.siftnews.delivery.application.port.in.DispatchJobRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class DispatchTriggerTest {

    private static final Instant NOW = Instant.parse("2026-08-08T03:00:00Z");

    private RecordingDispatchJobRunner dispatchJobRunner;
    private RecordingIssueCatalog issueCatalog;
    private DispatchTrigger trigger;

    @BeforeEach
    void setUp() {
        dispatchJobRunner = new RecordingDispatchJobRunner();
        issueCatalog = new RecordingIssueCatalog();
        issueCatalog.references.add(new ScheduledIssueReference(11L, 12L));
        issueCatalog.references.add(new ScheduledIssueReference(21L, 22L));
        trigger = new DispatchTrigger(dispatchJobRunner, issueCatalog,
                Clock.fixed(NOW, ZoneId.of("UTC")), "Asia/Seoul");
    }

    @Test
    void launchesEachScheduledIssueWithCurrentLocalHour() {
        trigger.triggerHourlyDispatch();

        assertThat(issueCatalog.requestedDates()).containsExactly(LocalDate.parse("2026-08-08"));
        assertThat(dispatchJobRunner.requests()).containsExactly(
                new DispatchRequest(11L, 12L, 12),
                new DispatchRequest(21L, 22L, 12));
    }

    @Test
    void swallowsRunnerFailureSoTheSchedulerSurvives() {
        dispatchJobRunner.failure = new IllegalStateException("기동 실패(테스트)");

        assertThatCode(() -> trigger.triggerHourlyDispatch()).doesNotThrowAnyException();
    }

    private record DispatchRequest(Long issueId, Long topicId, int preferredSendHour) {
    }

    private static final class RecordingDispatchJobRunner implements DispatchJobRunner {

        private final List<DispatchRequest> requests = new ArrayList<>();
        private Exception failure;

        @Override
        public DispatchJobRunSummary run(Long issueId, Long topicId, int preferredSendHour) throws Exception {
            if (failure != null) {
                throw failure;
            }
            requests.add(new DispatchRequest(issueId, topicId, preferredSendHour));
            return new DispatchJobRunSummary(1L, "COMPLETED", "COMPLETED", 0, 0, 0);
        }

        private List<DispatchRequest> requests() {
            return requests;
        }
    }

    private static final class RecordingIssueCatalog implements IssueCatalog {

        private final List<ScheduledIssueReference> references = new ArrayList<>();
        private final List<LocalDate> requestedDates = new ArrayList<>();

        @Override
        public List<ScheduledIssueReference> findScheduled(LocalDate runDate) {
            requestedDates.add(runDate);
            return references;
        }

        @Override
        public Optional<com.siftnews.content.api.NewsletterIssue> findNewsletterIssue(Long issueId) {
            return Optional.empty();
        }

        private List<LocalDate> requestedDates() {
            return requestedDates;
        }
    }
}
