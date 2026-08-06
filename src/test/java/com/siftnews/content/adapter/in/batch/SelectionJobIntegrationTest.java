package com.siftnews.content.adapter.in.batch;

import com.siftnews.source.application.port.out.SaveArticlePort;
import com.siftnews.source.domain.Article;
import com.siftnews.source.domain.Category;
import com.siftnews.source.domain.RawArticle;
import com.siftnews.support.AbstractIntegrationTest;
import com.siftnews.support.TestDatabaseFixtures;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigInteger;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * selectionJob 조립 통합 테스트 — <b>M2 공통 DoD "시드 토픽으로 이슈 1건 생성 확인"</b>.
 * <p>
 * fake로 대체하지 않고 실 DB에 토픽·기사를 심어 세 Step을 그대로 돌린다 — #29에서 후보 조회·
 * 클러스터 갱신이 실 어댑터로 배선됐으므로, 이제 수집된 기사에서 이슈까지 한 번에 이어지는지
 * 확인할 수 있다.
 */
@SpringBatchTest
class SelectionJobIntegrationTest extends AbstractIntegrationTest {

    private static final String RELEVANT_BODY = "Spring 배치 이야기. ".repeat(30);
    /** 토픽 키워드가 <b>본문에도</b> 없어야 필터에서 탈락한다 — 제목만 다르게 해선 걸러지지 않는다. */
    private static final String UNRELATED_BODY = "환율과 금리 동향 분석. ".repeat(30);
    private static final ZoneId SELECTION_ZONE = ZoneId.of("Asia/Seoul");

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    /** Job 빈이 collectionJob·selectionJob 둘이라 어느 것을 돌릴지 명시해야 한다. */
    @Autowired
    @Qualifier("selectionJob")
    private Job selectionJob;

    @Autowired
    private SaveArticlePort saveArticlePort;

    @Autowired
    private EntityManager entityManager;

    /**
     * 테스트 전체를 {@code @Transactional}로 감쌀 수 없다 — 배치 Step은 자기 트랜잭션에서 돌기 때문에
     * 테스트 트랜잭션 안의 미커밋 데이터를 보지 못한다. 그래서 준비·검증 구문만 따로 묶는다.
     */
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private Clock clock;

    private Long topicId;
    private Instant from;
    private Instant to;
    private long launchSequence;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils.setJob(selectionJob);
        transactionTemplate.executeWithoutResult(status -> {
            entityManager.createNativeQuery("delete from issue_item").executeUpdate();
            entityManager.createNativeQuery("delete from issue").executeUpdate();
            entityManager.createNativeQuery("delete from article_score").executeUpdate();
            entityManager.createNativeQuery("delete from article").executeUpdate();
            entityManager.createNativeQuery("delete from topic").executeUpdate();
            TestDatabaseFixtures.source(entityManager, 7L);
        });
        topicId = transactionTemplate.execute(status -> insertDevTopic());
        launchSequence = 0;
    }

    /** 토픽 저장 포트가 없어(시더는 {@code @Profile("!test")}) 네이티브로 심는다. */
    private Long insertDevTopic() {
        entityManager.createNativeQuery("""
                        insert into topic (created_at, updated_at, name, slug, lang_scope,
                            include_keywords, exclude_keywords, keyword_weights, source_categories,
                            recency_half_life_hours, max_items, score_threshold, active)
                        values (now(), now(), '개발', 'dev', 'ko,en',
                            cast('["Spring"]' as jsonb), cast('[]' as jsonb), cast('{}' as jsonb),
                            cast('["DEV"]' as jsonb), 24, 10, 0.0, true)
                        """)
                .executeUpdate();
        Object id = entityManager.createNativeQuery("select id from topic where slug = 'dev'").getSingleResult();
        return ((Number) id).longValue();
    }

    private void saveArticles() {
        saveArticlePort.saveNew(List.of(
                article("https://ex.com/spring-batch", "Spring 배치 튜닝", RELEVANT_BODY),
                article("https://ex.com/spring-boot", "Spring Boot 3.5 정리", RELEVANT_BODY),
                article("https://ex.com/unrelated", "환율 급등 분석", UNRELATED_BODY)));
        // 윈도우는 created_at 기준이라, 고정 시각을 기준으로 기사를 감싸도록 잡는다.
        to = clock.instant().plusSeconds(60);
        from = to.minusSeconds(3600);
    }

    private Article article(String url, String title, String body) {
        return Article.create(new RawArticle(url, title, body, "ko",
                clock.instant().minusSeconds(600), Category.DEV), 7L);
    }

    private JobParameters parameters() {
        return parameters(LocalDate.ofInstant(clock.instant(), SELECTION_ZONE));
    }

    private JobParameters parameters(LocalDate runDate) {
        return new JobParametersBuilder()
                .addLong("topicId", topicId)
                .addString("runDate", runDate.toString())
                .addString("from", from.toString())
                .addString("to", to.toString())
                .addLong("launchedAt", ++launchSequence)
                .toJobParameters();
    }

    private long count(String table) {
        return transactionTemplate.execute(status -> {
            Object value = entityManager.createNativeQuery("select count(*) from " + table).getSingleResult();
            return value instanceof BigInteger big ? big.longValue() : ((Number) value).longValue();
        });
    }

    /** 수집된 기사 → 정규화·클러스터링 → 스코어링 → 이슈 생성까지 한 번에 이어지는지. */
    @Test
    void buildsIssueFromCollectedArticles() throws Exception {
        saveArticles();

        JobExecution execution = jobLauncherTestUtils.launchJob(parameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getStepExecutions()).extracting("stepName")
                .containsExactly("normalizeDedupStep", "scoreStep", "selectStep");
        assertThat(count("issue")).isEqualTo(1);
        // 토픽 키워드가 'Spring'이라 환율 기사는 필터에서 탈락한다.
        assertThat(count("issue_item")).isEqualTo(2);
        assertThat(count("article_score")).isEqualTo(2);
    }

    /**
     * <b>#35 회귀 방어.</b> 운영 존 KST의 00:00~09:00에는 {@code runDate}(KST 날짜)가 <b>UTC 기준으로
     * 아직 내일</b>이다 — 확정 발행 시각 06:00 KST가 정확히 이 구간이다. 옛 구현은 점수 조회 하한을
     * {@code runDate}의 UTC 자정으로 잡아, 이 구간에서는 방금 계산한 점수가 전부 하한 미만이 되어
     * <b>매일 빈 호</b>가 나갔다.
     * <p>
     * 그 상태를 <b>{@code runDate}로 직접 만들어</b> 고정한다. 벽시계에 맡기면 KST 09:00~24:00에 돌린
     * 실행은 결함을 그냥 통과시킨다 — 실제로 이 이슈를 잡은 첫 실행도 로컬이 우연히 새벽이라 걸렸고,
     * CI(UTC)는 계속 녹색이었다. 테스트 존을 고정해도(build.gradle) <b>시각 의존은 남으므로</b>
     * 재현 조건을 데이터로 못 박는다.
     */
    @Test
    void buildsIssueWhenRunDateIsAheadOfUtcDate() throws Exception {
        saveArticles();

        JobExecution execution = jobLauncherTestUtils.launchJob(
                parameters(LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC).plusDays(1)));

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(count("article_score")).isEqualTo(2);
        // 하한이 runDate에서 유도되면 여기가 0건이 된다.
        assertThat(count("issue_item")).isEqualTo(2);
    }

    /** 클러스터 id가 실제로 article에 반영돼야 화제성 점수가 의미를 갖는다 (#29 배선 확인). */
    @Test
    void persistsDedupClusterIdOnArticles() throws Exception {
        saveArticles();

        jobLauncherTestUtils.launchJob(parameters());

        assertThat(count("article where dedup_cluster_id is not null")).isEqualTo(3);
    }

    /**
     * 같은 날 재기동은 정상 경로다(배치 재시도·수동 재실행) — 호가 두 개 생기면 구독자에게
     * 같은 날 두 통이 나간다. 세 Step 모두 재실행 멱등이라야 한다.
     */
    @Test
    void rerunDoesNotDuplicateIssue() throws Exception {
        saveArticles();

        jobLauncherTestUtils.launchJob(parameters());
        JobExecution second = jobLauncherTestUtils.launchJob(parameters());

        assertThat(second.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(count("issue")).isEqualTo(1);
        assertThat(count("issue_item")).isEqualTo(2);
        assertThat(count("article_score")).isEqualTo(2);
    }

    /** 후보가 없어도 Job은 성공하고 빈 호를 남긴다 — "아직 안 돌았다"와 구분되어야 한다. */
    @Test
    void completesWithEmptyIssueWhenNoCandidates() throws Exception {
        to = clock.instant();
        from = to.minusSeconds(3600);

        JobExecution execution = jobLauncherTestUtils.launchJob(parameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(count("issue")).isEqualTo(1);
        assertThat(count("issue_item")).isZero();
    }
}
