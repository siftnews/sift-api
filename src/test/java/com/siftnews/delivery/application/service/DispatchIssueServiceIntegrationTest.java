package com.siftnews.delivery.application.service;

import com.siftnews.delivery.application.port.in.DispatchIssueUseCase;
import com.siftnews.support.AbstractIntegrationTest;
import com.siftnews.support.TestDatabaseFixtures;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class DispatchIssueServiceIntegrationTest extends AbstractIntegrationTest {

    private static final long TOPIC_ID = 900_001L;
    private static final long ISSUE_ID = 900_002L;
    private static final long SUBSCRIBER_ID = 900_003L;
    private static final int SEND_HOUR = 9;

    @Autowired
    private DispatchIssueUseCase dispatchIssueUseCase;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void rerunCreatesOneDeliveryJobAndTask() {
        prepareSnapshotTarget();

        dispatchIssueUseCase.dispatch(ISSUE_ID, TOPIC_ID, SEND_HOUR);
        dispatchIssueUseCase.dispatch(ISSUE_ID, TOPIC_ID, SEND_HOUR);

        assertThat(count("delivery_job")).isEqualTo(1);
        assertThat(count("delivery_task")).isEqualTo(1);
    }

    private void prepareSnapshotTarget() {
        TestDatabaseFixtures.topic(entityManager, TOPIC_ID);
        entityManager.createNativeQuery("""
                INSERT INTO issue (id, created_at, updated_at, topic_id, run_date, title, status)
                VALUES (:id, now(), now(), :topicId, :runDate, 'Fixture issue', 'SCHEDULED')
                """)
                .setParameter("id", ISSUE_ID)
                .setParameter("topicId", TOPIC_ID)
                .setParameter("runDate", LocalDate.parse("2026-08-07"))
                .executeUpdate();
        entityManager.createNativeQuery("""
                INSERT INTO subscriber (id, created_at, updated_at, email, status, preferred_send_hour)
                VALUES (:id, now(), now(), 'snapshot@example.com', 'ACTIVE', :sendHour)
                """)
                .setParameter("id", SUBSCRIBER_ID)
                .setParameter("sendHour", SEND_HOUR)
                .executeUpdate();
        entityManager.createNativeQuery("""
                INSERT INTO subscription (created_at, updated_at, subscriber_id, topic_id, status)
                VALUES (now(), now(), :subscriberId, :topicId, 'ACTIVE')
                """)
                .setParameter("subscriberId", SUBSCRIBER_ID)
                .setParameter("topicId", TOPIC_ID)
                .executeUpdate();
        entityManager.flush();
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }
}
