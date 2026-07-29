package com.siftnews.source.adapter.out.persistence;

import com.siftnews.content.application.port.out.LoadCandidateArticlesPort;
import com.siftnews.content.application.port.out.UpdateArticleClusterPort;
import com.siftnews.content.domain.CandidateArticle;
import com.siftnews.source.api.ArticleCandidate;
import com.siftnews.source.api.ArticleCatalog;
import com.siftnews.source.domain.Category;
import com.siftnews.support.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 선별 후보 조회·클러스터 갱신의 <b>실 DB 검증</b> — #19~#27 내내 fake로만 확인되던 경로다.
 * <p>
 * source 패키지에 두는 이유: 기사를 심으려면 package-private인 {@link ArticleJpaRepository}가
 * 필요한데, 검증 대상인 경계는 content 쪽 포트까지 이어지므로 두 모듈의 공개 포트를 함께 부른다.
 */
@Transactional
class ArticleQueryAdapterTest extends AbstractIntegrationTest {

    private static final Instant FROM = Instant.parse("2026-07-28T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-07-29T00:00:00Z");

    @Autowired
    private ArticleJpaRepository articleJpaRepository;

    @Autowired
    private ArticleCatalog articleCatalog;

    @Autowired
    private LoadCandidateArticlesPort loadCandidateArticlesPort;

    @Autowired
    private UpdateArticleClusterPort updateArticleClusterPort;

    @Autowired
    private EntityManager entityManager;

    /**
     * {@code created_at}은 JPA Auditing이 저장 시각으로 채우므로, 윈도우 경계를 검증하려면
     * 저장 후 원하는 시각으로 바꿔야 한다.
     */
    private Long saveArticleCreatedAt(String suffix, Instant createdAt) {
        ArticleJpaEntity saved = articleJpaRepository.save(new ArticleJpaEntity(
                7L, "https://ex.com/" + suffix, "https://ex.com/" + suffix, "제목 " + suffix,
                "본문", "ko", Instant.parse("2026-07-28T06:00:00Z"), Category.DEV));
        articleJpaRepository.flush();
        entityManager.createNativeQuery("update article set created_at = ?1 where id = ?2")
                .setParameter(1, createdAt)
                .setParameter(2, saved.getId())
                .executeUpdate();
        entityManager.clear();
        return saved.getId();
    }

    /**
     * 반열림 구간 {@code [from, to)} — <b>from은 포함, to는 미포함</b>이어야 한다. 이 경계가
     * 어긋나면 인접한 두 실행이 같은 기사를 겹쳐 보거나(중복 게재) 사이로 흘려버린다(누락).
     */
    @Test
    void loadsHalfOpenWindowIncludingFromExcludingTo() {
        Long justBefore = saveArticleCreatedAt("before", FROM.minusMillis(1));
        Long atFrom = saveArticleCreatedAt("at-from", FROM);
        Long inside = saveArticleCreatedAt("inside", FROM.plusSeconds(3600));
        Long atTo = saveArticleCreatedAt("at-to", TO);

        List<ArticleCandidate> found = articleCatalog.findCandidates(FROM, TO);

        assertThat(found).extracting(ArticleCandidate::articleId)
                .contains(atFrom, inside)
                .doesNotContain(justBefore, atTo);
    }

    /** content 포트를 통해서도 같은 윈도우가 도메인 타입으로 넘어와야 한다. */
    @Test
    void contentPortReceivesMappedDomainView() {
        Long articleId = saveArticleCreatedAt("mapped", FROM.plusSeconds(60));

        List<CandidateArticle> candidates = loadCandidateArticlesPort.loadCandidates(FROM, TO);

        assertThat(candidates).singleElement().satisfies(candidate -> {
            assertThat(candidate.articleId()).isEqualTo(articleId);
            assertThat(candidate.sourceId()).isEqualTo(7L);
            assertThat(candidate.category()).isEqualTo("DEV");
            assertThat(candidate.title()).isEqualTo("제목 mapped");
            assertThat(candidate.dedupClusterId()).isNull();
        });
    }

    /**
     * 부여와 해제가 한 호출에 섞여 들어온다 — 선별은 매 실행이 윈도우 후보 전체의 상태를
     * 통째로 교체하기 때문이다(D-031). null 값이 해제로 반영되지 않으면 이전 실행의
     * 클러스터가 살아남는다.
     */
    @Test
    void updatesAndClearsClustersInOneCall() {
        Long keeps = saveArticleCreatedAt("keeps", FROM.plusSeconds(10));
        Long joins = saveArticleCreatedAt("joins", FROM.plusSeconds(20));
        Long cleared = saveArticleCreatedAt("cleared", FROM.plusSeconds(30));
        Map<Long, String> initial = new HashMap<>();
        initial.put(cleared, "c-old");
        updateArticleClusterPort.updateClusters(initial);

        Map<Long, String> next = new HashMap<>();
        next.put(keeps, "c-1");
        next.put(joins, "c-1");
        next.put(cleared, null);
        updateArticleClusterPort.updateClusters(next);

        Map<Long, String> actual = articleCatalog.findCandidates(FROM, TO).stream()
                .collect(HashMap::new, (m, c) -> m.put(c.articleId(), c.dedupClusterId()), HashMap::putAll);
        assertThat(actual.get(keeps)).isEqualTo("c-1");
        assertThat(actual.get(joins)).isEqualTo("c-1");
        assertThat(actual.get(cleared)).isNull();
    }

    /** 빈 맵으로 내려가면 어댑터의 {@code IN ()}이 SQL 문법 오류를 낸다 — 서비스가 막아야 한다. */
    @Test
    void emptyUpdateIsNoOp() {
        updateArticleClusterPort.updateClusters(Map.of());

        assertThat(articleCatalog.findCandidates(FROM, TO)).isEmpty();
    }

    @Test
    void rejectsInvalidWindow() {
        assertThat(articleCatalog.findCandidates(FROM, TO)).isEmpty();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> articleCatalog.findCandidates(TO, FROM))
                .isInstanceOf(com.siftnews.source.domain.ArticleException.class);
    }
}
