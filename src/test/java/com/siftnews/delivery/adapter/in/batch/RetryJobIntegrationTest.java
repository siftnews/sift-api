package com.siftnews.delivery.adapter.in.batch;

import com.siftnews.delivery.application.port.in.DispatchIssueUseCase;
import com.siftnews.delivery.application.port.out.UpdateDeliveryTaskPort;
import com.siftnews.support.AbstractIntegrationTest;
import com.siftnews.support.TestDatabaseFixtures;
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

import java.time.Instant;
import java.time.LocalDate;
import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.assertThat;

/** #62 회귀: 도래한 FAILED task를 외부 SMTP 없이 재시도해 SENT로 종결한다. */
@SpringBatchTest
class RetryJobIntegrationTest extends AbstractIntegrationTest {

    private static final long SOURCE_ID = 620_001L;
    private static final long TOPIC_ID = 620_002L;
    private static final long ISSUE_ID = 620_003L;
    private static final long ARTICLE_ID = 620_004L;
    private static final long SUBSCRIBER_ID = 620_005L;
    private static final int SEND_HOUR = 9;
    private static final Instant FIXED_NOW = Instant.parse("2026-08-05T21:00:00Z");

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @Qualifier("retryJob")
    private Job retryJob;

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
        jobLauncherTestUtils.setJob(retryJob);
        transactionTemplate.executeWithoutResult(status -> prepareDispatchTarget());
    }

    @AfterEach
    void tearDown() {
        transactionTemplate.executeWithoutResult(status -> deleteDispatchTarget());
    }

    @Test
    void retriesDueFailedTaskAndClearsRetryMetadata() throws Exception {
        dispatchIssueUseCase.dispatch(ISSUE_ID, TOPIC_ID, SEND_HOUR);
        Long taskId = taskId();
        jdbcTemplate.update("""
                update delivery_task
                set status = 'FAILED', attempt_count = 1,
                    next_retry_at = ?, last_error = 'temporary failure'
                where id = ?
                """, Timestamp.from(FIXED_NOW.minusSeconds(1)), taskId);

        JobExecution execution = jobLauncherTestUtils.launchJob(new JobParametersBuilder()
                .addLong(RetryJobParameters.LAUNCHED_AT, 620_001L)
                .toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getStepExecutions()).extracting("stepName")
                .containsExactly("retryStep");
        assertThat(jdbcTemplate.queryForObject("select status from delivery_task where id = ?",
                String.class, taskId)).isEqualTo("SENT");
        assertThat(jdbcTemplate.queryForObject("select attempt_count from delivery_task where id = ?",
                Integer.class, taskId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select next_retry_at from delivery_task where id = ?",
                Object.class, taskId)).isNull();
        assertThat(jdbcTemplate.queryForObject("select last_error from delivery_task where id = ?",
                String.class, taskId)).isNull();
    }

    @Test
    void failedClaimIsConditionalOnDueTimeAndStatus() {
        dispatchIssueUseCase.dispatch(ISSUE_ID, TOPIC_ID, SEND_HOUR);
        Long taskId = taskId();
        jdbcTemplate.update("""
                update delivery_task
                set status = 'FAILED', attempt_count = 1, next_retry_at = ?
                where id = ?
                """, Timestamp.from(FIXED_NOW.plusSeconds(60)), taskId);

        assertThat(updateDeliveryTaskPort.claimFailed(taskId, FIXED_NOW, 3)).isZero();

        jdbcTemplate.update("update delivery_task set next_retry_at = ? where id = ?",
                Timestamp.from(FIXED_NOW.minusSeconds(1)), taskId);
        assertThat(updateDeliveryTaskPort.claimFailed(taskId, FIXED_NOW, 3)).isEqualTo(1);
        assertThat(updateDeliveryTaskPort.claimFailed(taskId, FIXED_NOW, 3)).isZero();
        assertThat(updateDeliveryTaskPort.markFailed(taskId, "again", 2, FIXED_NOW.plusSeconds(120)))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select status from delivery_task where id = ?",
                String.class, taskId)).isEqualTo("FAILED");
        assertThat(jdbcTemplate.queryForObject("select attempt_count from delivery_task where id = ?",
                Integer.class, taskId)).isEqualTo(2);
    }

    private Long taskId() {
        return jdbcTemplate.queryForObject("select id from delivery_task where subscriber_id = ?",
                Long.class, SUBSCRIBER_ID);
    }

    private void prepareDispatchTarget() {
        deleteDispatchTarget();

        TestDatabaseFixtures.source(entityManager, SOURCE_ID);
        TestDatabaseFixtures.topic(entityManager, TOPIC_ID);
        entityManager.createNativeQuery("""
                        insert into article (id, created_at, updated_at, source_id, url, normalized_url, title)
                        values (:articleId, now(), now(), :sourceId, 'https://example.com/retry-article',
                            'https://example.com/retry-article', '재시도할 기사')
                        """)
                .setParameter("articleId", ARTICLE_ID)
                .setParameter("sourceId", SOURCE_ID)
                .executeUpdate();
        entityManager.createNativeQuery("""
                        insert into issue (id, created_at, updated_at, topic_id, run_date, title, status)
                        values (:issueId, now(), now(), :topicId, :runDate, '재시도할 이슈', 'SCHEDULED')
                        """)
                .setParameter("issueId", ISSUE_ID)
                .setParameter("topicId", TOPIC_ID)
                .setParameter("runDate", LocalDate.parse("2026-08-05"))
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
                        values (:subscriberId, now(), now(), 'retry-reader@example.com', 'ACTIVE', :sendHour)
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
