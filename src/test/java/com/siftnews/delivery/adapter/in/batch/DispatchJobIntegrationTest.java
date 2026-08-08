package com.siftnews.delivery.adapter.in.batch;

import com.siftnews.support.AbstractIntegrationTest;
import com.siftnews.support.TestDatabaseFixtures;
import com.siftnews.delivery.application.port.in.DispatchIssueUseCase;
import com.siftnews.delivery.application.port.out.UpdateDeliveryTaskPort;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** #60 회귀: 스냅샷부터 테스트 프로필의 무외부 SMTP 발송까지 한 Job으로 검증한다. */
@SpringBatchTest
class DispatchJobIntegrationTest extends AbstractIntegrationTest {

    private static final long SOURCE_ID = 610_001L;
    private static final long TOPIC_ID = 610_002L;
    private static final long ISSUE_ID = 610_003L;
    private static final long ARTICLE_ID = 610_004L;
    private static final long SUBSCRIBER_ID = 610_005L;
    private static final int SEND_HOUR = 9;

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @Qualifier("dispatchJob")
    private Job dispatchJob;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private DispatchIssueUseCase dispatchIssueUseCase;

    @Autowired
    private UpdateDeliveryTaskPort updateDeliveryTaskPort;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils.setJob(dispatchJob);
        transactionTemplate.executeWithoutResult(status -> prepareDispatchTarget());
    }

    @AfterEach
    void tearDown() {
        transactionTemplate.executeWithoutResult(status -> deleteDispatchTarget());
    }

    @Test
    void sendsPendingTaskAndRecordsSentStatus() throws Exception {
        JobExecution execution = jobLauncherTestUtils.launchJob(new JobParametersBuilder()
                .addLong(DispatchJobParameters.ISSUE_ID, ISSUE_ID)
                .addLong(DispatchJobParameters.TOPIC_ID, TOPIC_ID)
                .addLong(DispatchJobParameters.SEND_HOUR, (long) SEND_HOUR)
                .addLong("launchedAt", System.nanoTime())
                .toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getStepExecutions()).extracting("stepName")
                .containsExactly("snapshotStep", "sendStep");
        assertThat(jdbcTemplate.queryForObject("select status from delivery_task where subscriber_id = ?",
                String.class, SUBSCRIBER_ID)).isEqualTo("SENT");
    }

    @Test
    void appliesConditionalTaskTransitions() {
        dispatchIssueUseCase.dispatch(ISSUE_ID, TOPIC_ID, SEND_HOUR);
        Long taskId = jdbcTemplate.queryForObject("select id from delivery_task where subscriber_id = ?",
                Long.class, SUBSCRIBER_ID);

        assertThat(updateDeliveryTaskPort.claimPending(taskId)).isEqualTo(1);
        assertThat(updateDeliveryTaskPort.claimPending(taskId)).isZero();
        assertThat(updateDeliveryTaskPort.markSent(taskId)).isEqualTo(1);
        assertThat(updateDeliveryTaskPort.markFailed(taskId, "should not overwrite sent", 2,
                java.time.Instant.parse("2026-08-07T10:00:00Z"))).isZero();
        assertThat(jdbcTemplate.queryForObject("select status from delivery_task where id = ?", String.class,
                taskId)).isEqualTo("SENT");
        assertThat(jdbcTemplate.queryForObject("select sent_at from delivery_task where id = ?", Object.class,
                taskId)).isNotNull();
    }

    @Test
    void recordsFailedTransitionAndError() {
        dispatchIssueUseCase.dispatch(ISSUE_ID, TOPIC_ID, SEND_HOUR);
        Long taskId = jdbcTemplate.queryForObject("select id from delivery_task where subscriber_id = ?",
                Long.class, SUBSCRIBER_ID);

        assertThat(updateDeliveryTaskPort.claimPending(taskId)).isEqualTo(1);
        assertThat(updateDeliveryTaskPort.markFailed(taskId, "smtp unavailable", 1,
                java.time.Instant.parse("2026-08-07T10:00:00Z"))).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select status from delivery_task where id = ?", String.class,
                taskId)).isEqualTo("FAILED");
        assertThat(jdbcTemplate.queryForObject("select last_error from delivery_task where id = ?", String.class,
                taskId)).isEqualTo("smtp unavailable");
    }

    private void prepareDispatchTarget() {
        deleteDispatchTarget();

        TestDatabaseFixtures.source(entityManager, SOURCE_ID);
        TestDatabaseFixtures.topic(entityManager, TOPIC_ID);
        entityManager.createNativeQuery("""
                        insert into article (id, created_at, updated_at, source_id, url, normalized_url, title)
                        values (:articleId, now(), now(), :sourceId, 'https://example.com/article',
                            'https://example.com/article', '발송할 기사')
                        """)
                .setParameter("articleId", ARTICLE_ID)
                .setParameter("sourceId", SOURCE_ID)
                .executeUpdate();
        entityManager.createNativeQuery("""
                        insert into issue (id, created_at, updated_at, topic_id, run_date, title, status)
                        values (:issueId, now(), now(), :topicId, :runDate, '발송할 이슈', 'SCHEDULED')
                        """)
                .setParameter("issueId", ISSUE_ID)
                .setParameter("topicId", TOPIC_ID)
                .setParameter("runDate", LocalDate.parse("2026-08-07"))
                .executeUpdate();
        entityManager.createNativeQuery("""
                        insert into issue_item (created_at, updated_at, issue_id, article_id, item_rank, score)
                        values (now(), now(), :issueId, :articleId, 1, 1.0)
                        """)
                .setParameter("issueId", ISSUE_ID)
                .setParameter("articleId", ARTICLE_ID)
                .executeUpdate();
        entityManager.createNativeQuery("""
                        insert into subscriber (id, created_at, updated_at, email, status, preferred_send_hour)
                        values (:subscriberId, now(), now(), 'reader@example.com', 'ACTIVE', :sendHour)
                        """)
                .setParameter("subscriberId", SUBSCRIBER_ID)
                .setParameter("sendHour", SEND_HOUR)
                .executeUpdate();
        entityManager.createNativeQuery("""
                        insert into subscription (created_at, updated_at, subscriber_id, topic_id, status)
                        values (now(), now(), :subscriberId, :topicId, 'ACTIVE')
                        """)
                .setParameter("subscriberId", SUBSCRIBER_ID)
                .setParameter("topicId", TOPIC_ID)
                .executeUpdate();
        entityManager.flush();
    }

    private void deleteDispatchTarget() {
        entityManager.createNativeQuery("delete from delivery_task where subscriber_id = :subscriberId")
                .setParameter("subscriberId", SUBSCRIBER_ID)
                .executeUpdate();
        entityManager.createNativeQuery("delete from delivery_job where issue_id = :issueId")
                .setParameter("issueId", ISSUE_ID)
                .executeUpdate();
        entityManager.createNativeQuery("delete from subscription where subscriber_id = :subscriberId")
                .setParameter("subscriberId", SUBSCRIBER_ID)
                .executeUpdate();
        entityManager.createNativeQuery("delete from subscriber where id = :subscriberId")
                .setParameter("subscriberId", SUBSCRIBER_ID)
                .executeUpdate();
        entityManager.createNativeQuery("delete from issue_item where issue_id = :issueId")
                .setParameter("issueId", ISSUE_ID)
                .executeUpdate();
        entityManager.createNativeQuery("delete from issue where id = :issueId")
                .setParameter("issueId", ISSUE_ID)
                .executeUpdate();
        entityManager.createNativeQuery("delete from article where id = :articleId")
                .setParameter("articleId", ARTICLE_ID)
                .executeUpdate();
        entityManager.createNativeQuery("delete from topic where id = :topicId")
                .setParameter("topicId", TOPIC_ID)
                .executeUpdate();
        entityManager.createNativeQuery("delete from source where id = :sourceId")
                .setParameter("sourceId", SOURCE_ID)
                .executeUpdate();
    }
}
