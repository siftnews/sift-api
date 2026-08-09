package com.siftnews.content.adapter.out.persistence;

import com.siftnews.content.api.ScheduledIssueReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

interface IssueJpaRepository extends JpaRepository<IssueJpaEntity, Long> {

    Optional<IssueJpaEntity> findByTopicIdAndRunDate(Long topicId, LocalDate runDate);

    /**
     * {@code (topic_id, run_date)}가 이미 있으면 제목을 갱신한다. 기존 DRAFT만 입력 상태로 승격한다.
     * <p>
     * 이미 발송 단계로 넘어간 호(SCHEDULED·SENDING·SENT)를 재실행이 DRAFT로 되돌리면, 발송 여부를
     * 상태로 판단하는 M3가 같은 호를 다시 보내게 된다. 되돌리지 않는다.
     * <p>
     * "조회 → 없으면 insert"로 나누지 않는 이유는 {@code SourceJpaRepository}와 같다 —
     * 동시에 돌면 둘 다 없다고 판정해 한쪽이 유니크 제약으로 죽는다.
     */
    @Modifying
    @Query(value = """
            INSERT INTO issue (topic_id, run_date, title, status, created_at, updated_at)
            VALUES (:topicId, :runDate, :title, :status, :now, :now)
            ON CONFLICT (topic_id, run_date) DO UPDATE SET
                title = EXCLUDED.title,
                status = CASE
                    WHEN issue.status = 'DRAFT' THEN EXCLUDED.status
                    ELSE issue.status
                END,
                updated_at = EXCLUDED.updated_at
            """, nativeQuery = true)
    void upsert(@Param("topicId") Long topicId,
                @Param("runDate") LocalDate runDate,
                @Param("title") String title,
                @Param("status") String status,
                @Param("now") Instant now);

    @Query("""
            select new com.siftnews.content.api.ScheduledIssueReference(issue.id, issue.topicId)
            from IssueJpaEntity issue
            where issue.runDate = :runDate and issue.status = 'SCHEDULED'
            """)
    List<ScheduledIssueReference> findScheduled(LocalDate runDate);
}
