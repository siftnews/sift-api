# Sift — MVP 상세 설계

> 상위 기획 [PLAN.md](https://github.com/siftnews/sift-docs/blob/main/PLAN.md) · 선별 상세 [SELECTION.md](./SELECTION.md) · 최종 수정: 2026-07-13 (M1-4 Source 영속 어댑터 반영, D-022)
>
> 범위: V1(베이스라인) 구현을 위한 ERD · 배치 Job/Step · 포트 시그니처

---

## 0. MVP 결정 요약

| 항목 | 값 |
|---|---|
| 토픽 시드 | 개발/엔지니어링, AI/머신러닝, 경제/금융/투자 (3개) |
| 소스 언어 | 한국어 + 영어 (article·source에 `lang`) |
| 발송 주기 | **매일(DAILY 고정) + 구독자 선호 시각** (`subscriber.preferred_send_hour`, D-019) |
| 선별 | 스코어링+랭킹 5단계 (SELECTION.md) |
| 발송 | 로컬 SMTP, 단일 스레드 청크 (V1) |

---

## 1. 토픽 시드 셋

> 키워드/소스는 초안. 구현 후 breakdown 로그 보며 튜닝.

| slug | name | lang | includeKeywords (예) | sourceCategories |
|---|---|---|---|---|
| `dev` | 개발/엔지니어링 | ko,en | Spring, Kotlin, Java, Kubernetes, 백엔드, 아키텍처, DevOps, 데이터베이스 | dev, programming |
| `ai` | AI/머신러닝 | ko,en | LLM, Claude, GPT, RAG, 에이전트, 파인튜닝, 트랜스포머, 추론 | ai, ml |
| `econ` | 경제/금융/투자 | ko,en | 금리, 환율, 반도체, 인플레이션, 연준, Fed, 코스피, 실적 | economy, finance |

> 발송 주기는 전 토픽 **DAILY 고정** — cadence는 토픽 속성이 아님 (D-019).

### 소스 시드 (RSS 9종 — 이슈 #23에서 확정)

> 원본은 `SourceSeedData` — 아래 표는 그 사본이 아니라 **설계 의도(토픽별 커버리지·언어 배분)** 를 남기는 자리다.
> 모든 url은 2026-07-26 실제 응답으로 검증했다 (HTTP 200 + RSS 루트 태그 + 최근 갱신).

| 토픽 | name | lang | category | url |
|---|---|---|---|---|
| dev | Hacker News | en | PROGRAMMING | https://news.ycombinator.com/rss |
| dev | 토스 기술블로그 | ko | DEV | https://toss.tech/rss.xml |
| dev | Ars Technica | en | DEV | https://feeds.arstechnica.com/arstechnica/index |
| ai | AI타임스 | ko | AI | https://www.aitimes.com/rss/allArticle.xml |
| ai | Import AI | en | AI | https://importai.substack.com/feed |
| ai | Google AI Blog | en | AI | https://blog.google/technology/ai/rss/ |
| econ | 한국경제 경제 | ko | ECONOMY | https://www.hankyung.com/feed/economy |
| econ | 매일경제 경제 | ko | ECONOMY | https://www.mk.co.kr/rss/30100041/ |
| econ | BBC Business | en | FINANCE | https://feeds.bbci.co.uk/news/business/rss.xml |

- **제외한 후보**: 우아한형제들·LINE 기술블로그·CNBC(403), Yahoo Finance(429), 연합뉴스 경제(연결 실패), Anthropic(404 — 공식 RSS 미제공), 카카오테크(200이나 최신 글 2026-06-23로 정체).
- **대용량 히스토리 피드 제외**: Hugging Face(831건)·OpenAI(1050건)는 전체 글을 담은 피드라 첫 수집에 수백 건이 한꺼번에 유입돼 e2e 확인을 방해한다 → 20건 규모 피드로 대체. 부하 측정용 데이터가 필요해지면 M4에서 재검토.
- **Atom 미지원 확인 필요**: 네이버 D2 등 Atom(`<feed>`/`<entry>`) 피드는 `rome`이 파싱은 하지만 `RssFeedAdapter` 테스트가 RSS 픽스처만 다뤄 미검증 — 후속 이슈.
- 피드 url은 **정규화하지 않고 원본 그대로** 저장한다 (`Source.create`). `UriNormalizer`는 **기사 동일성 판정 전용**이다 — 추적 파라미터(`utm_*`·`at_*`·`fbclid` …)를 걷어내는데, 피드 url에서는 그중 무엇이 피드를 구분하는 파라미터인지 알 수 없다(`?feed=rss2`·`?id=02`처럼 쿼리로 피드를 나누는 사이트가 있다). 재사용하면 다른 피드를 가리킬 위험만 진다.

---

## 2. ERD

```
source ──1:N──▶ article ──1:N──▶ article_score ◀──N:1── topic
                   │                                       │
                   └──1:N──▶ issue_item ◀──N:1── issue ◀──1:N── topic
                                                  │
subscriber ──1:N──▶ subscription ◀──N:1── topic   │
     │                                            │
     └──────────1:N──────────▶ delivery_task ◀─N:1┘(via delivery_job)
                                   ▲
                          delivery_job ──1:N──┘
                              ▲
                          issue ──1:N── delivery_job
```

### 테이블 정의

```
source        (id, name, type[RSS|API], url, lang, category,
               trust_score, active, last_crawled_at)
              -- trust_score는 M1-4 영속 어댑터 범위에서 제외 — 도메인·엔티티 미구현,
              -- M2 스코어링 구현 시 도메인과 함께 추가 재검토 (D-022 비고)

article       (id, source_id FK, url, normalized_url, title, body, lang,
               published_at, category, dedup_cluster_id, created_at)
              · UNIQUE(normalized_url)
              -- dedup_cluster_id는 Content가 매기고 Source가 보관만 한다. 도메인 Article에는
              -- 두지 않는다 — 애그리거트가 이 값으로 아무 결정도 하지 않아 빈 필드가 된다 (이슈 #29)

topic         (id, name, slug, lang_scope,
               include_keywords, exclude_keywords, keyword_weights[json],
               source_categories, recency_half_life_hours, max_items,
               score_threshold, active)
              -- cadence·send_at_hour 제거 (D-019: 매일 고정 + 구독자 선호 시각)

article_score (id, article_id FK, source_id, topic_id FK, score, breakdown[json], computed_at)
              · UNIQUE(article_id, topic_id)
              -- source_id는 랭킹의 소스 쏠림 완화가 쓰는 비정규화 값 — article은 Source
              -- 소유(D-018)라 content가 조인할 수 없고, 점수 계산 시점엔 이미 아는 값이다

subscriber    (id, email, status[ACTIVE|UNSUB|BOUNCED],
               preferred_send_hour,                     -- 0~23, 구독자 선택 수신 시각 (D-019)
               created_at)
              · UNIQUE(email) · INDEX(status, preferred_send_hour)

subscription  (id, subscriber_id FK, topic_id FK, status[ACTIVE|PAUSED], created_at)
              · UNIQUE(subscriber_id, topic_id)

issue         (id, topic_id FK, run_date, title, status[DRAFT|SCHEDULED|SENDING|SENT],
               scheduled_at, published_at, created_at)
              · UNIQUE(topic_id, run_date)   -- 같은 날 같은 토픽에 호가 두 개 생기면
                                             -- 구독자에게 같은 날 두 통이 나간다 (이슈 #27)

issue_item    (id, issue_id FK, article_id FK, rank, score)
              · UNIQUE(issue_id, article_id)

delivery_job  (id, issue_id FK, total_count, status[CREATED|SENDING|DONE], created_at)

delivery_task (id, delivery_job_id FK, subscriber_id FK, email,
               status[PENDING|SENDING|SENT|FAILED|DEAD],
               attempt_count, next_retry_at, last_error, sent_at, idempotency_key)
              · UNIQUE(idempotency_key)  -- = hash(delivery_job_id, subscriber_id)
              · INDEX(status, next_retry_at)
```

---

## 3. 배치 Job / Step 명세

4개 Job + 1개 스케줄 트리거. 각 Spring Batch Job은 **인바운드 어댑터**로 UseCase를 호출.

### ① collectionJob — 수집 (collectionTrigger로 기동 — **매시 10분 확정, 이슈 #33**)
```
Step collectStep (chunk = 50)
  reader     LoadActiveSourcesPort → 활성 소스 목록
  processor  FetchFeedPort(RSS 파싱) → 신규 기사 추출 + Normalize(URL/본문/lang)
  writer     SaveArticlePort (UNIQUE normalized_url로 중복 무시)

- 설정: sift.collection.cron / zone
- Job 파라미터: launchedAt (기동 시각 millis)
```
> 주기 기본값을 **매시 정각이 아닌 10분**으로 둔 것은 정각이 수집기가 몰리는 시각이기 때문이다.
>
> `launchedAt`은 매 주기를 새 JobInstance로 만들기 위한 **식별 파라미터**다. 없으면 collectionJob은 파라미터가 빈 채로 기동돼 매번 같은 JobInstance가 되고, 두 번째 주기부터 `JobInstanceAlreadyCompleteException`으로 거부된다. 수집은 `UNIQUE(normalized_url)` 중복 무시 저장이라 다시 돌아도 결과가 덧나지 않는다.
>
> 기동 실패는 삼켜 로그만 남기고 다음 주기에 다시 시도한다 — 예외가 스케줄러 스레드까지 올라가면 이후 주기가 통째로 끊긴다. 소스별 fetch 실패는 여기까지 오지 않고 collectStep의 skip으로 격리된다.
>
> **스케줄러 풀 크기는 트리거 수만큼 확보한다** (`spring.task.scheduling.pool.size`). 기본값 1이면 수집·선별 트리거가 한 스레드를 두고 줄을 서, 06:00에 수집이 물려 있으면 그날 발행이 그만큼 밀린다.

### ② selectionTrigger — 일일 발행 트리거 (@Scheduled — **매일 06:00 Asia/Seoul 확정, 이슈 #31**)
```
- 전체 active 토픽 대상 (D-019: 전 토픽 DAILY 고정)
- 각 토픽에 대해 selectionJob 실행 파라미터(topicId, runDate, from, to) 투입 → 당일 이슈 생성
- 설정: sift.selection.cron / zone / window-hours (기본 24h)
```
> **윈도우를 트리거가 한 번만 계산해 전 토픽에 같은 값으로 넘긴다** — 이것이 D-032 불변식 (3)("한 runDate의 모든 selectionJob 실행은 동일한 `[from, to)`를 공유")을 코드 구조로 보장하는 지점이다. 각 Job이 스스로 "지금"을 기준으로 잡으면 먼저 도는 토픽과 나중 토픽의 윈도우가 어긋나, 나중 토픽이 앞선 토픽의 스코어링 전제를 덮어쓴다.
>
> 윈도우 크기 **24h**는 발행이 하루 한 번이고 `topic.recency_half_life_hours` 기본값도 24라 정렬을 맞춘 값이다 (D-031·D-032가 "M2-5에서 확정"으로 남긴 항목).
>
> 한 토픽의 기동 실패는 나머지 토픽 발행을 막지 않는다 — 수집의 소스별 skip 격리와 같은 방침.

### ③ selectionJob — 선별 (토픽별, 트리거로 기동)
```
Step normalizeDedupStep  (tasklet)  [from, to) 윈도우 후보 전체 재계산 + dedup_cluster_id 교체
Step scoreStep           (tasklet)  토픽 키워드/최신성/화제성/신뢰도 → article_score 저장
Step selectStep          (tasklet)  threshold·랭킹·소스 다양성 → issue(DRAFT) + issue_item
  → in: NormalizeDedupUseCase / ScoreArticlesUseCase / BuildIssueUseCase
```
> **세 Step 모두 tasklet이다 (이슈 #31에서 chunk → tasklet 정정).** 선별은 "윈도우 후보 전체를 다시 계산해 상태를 통째로 교체"하는 것이 멱등성의 전제라(D-031) 아이템 단위로 쪼갤 수 없다. 후보량이 커져 성능 로드맵에 오르면 재검토한다. tasklet이라 Step의 read/write count가 0이므로, 건수는 각 서비스가 자기 로그로 남기고 `SelectionMetricsListener`는 **단계별 소요시간**을 책임진다.
>
> `normalizeDedupStep`은 토픽 독립 전역 단계인데 selectionJob이 토픽마다 기동되므로 **토픽 수만큼 반복 실행**된다. 윈도우가 고정돼 있고 재실행이 멱등이라(D-031) 결과는 같다 — 중복 실행 비용이 측정으로 드러나면 전역 Job으로 분리한다.
>
> `DRAFT`→`SCHEDULED` 전이는 선별이 아니라 M3 발송 범위다.
> MVP는 토픽 단일 처리. 성능 V3에서 토픽 파티셔닝으로 전환.
>
> ⚠️ **윈도우 불변식 (D-031 — M2-5 배선 시 반드시 지킬 것).** `normalizeDedupStep`은 토픽 독립 전역 단계인데 selectionJob은 토픽마다 기동되므로(위 ②), 다음 3가지가 깨지면 클러스터가 분열돼 조용히 틀린 결과가 나온다.
> 1. `dedup_cluster_id`는 **그 기사를 마지막으로 포함한 실행의 윈도우 기준 결과**다 — 윈도우가 다른 값끼리는 비교·집계할 수 없다.
> 2. **scoreStep·selectStep의 대상 집합은 직전 `normalizeDedupStep` 윈도우의 부분집합**이어야 한다. 넓히면 윈도우 밖으로 밀려난 기사가 옛 clusterId를 유지한 채 남아, 같은 사건이 서로 다른 클러스터로 보인다 → trendScore 과소 계산 + 다양성(MMR) 페널티 미적용으로 같은 사건 기사가 한 이슈에 중복 게재된다.
> 3. **한 runDate의 모든 selectionJob 실행은 동일한 `[from, to)`를 공유**해야 한다 — 토픽별로 다르게 잡으면 나중 토픽이 앞선 토픽의 스코어링 전제를 덮어쓴다.
>
> 윈도우 기준 컬럼은 **`article.created_at`(수집 시각)** — `published_at`은 nullable이라 조회 누락이 생기고, 뒤늦게 수집된 과거 기사가 영영 클러스터링되지 않는다. (대표 선정은 별개로 `published_at` 기준 유지.)

### ④ dispatchJob — 발송 스냅샷 + 전송 (@Scheduled 매시 정각 — D-019 pull 스캔)
```
Step snapshotStep               당일 SCHEDULED issue × preferred_send_hour=현재 시각인
                                토픽 ACTIVE 구독자 → delivery_job 생성 + delivery_task(PENDING)
  → in: DispatchIssueUseCase.dispatch(issueId)   (멱등: idempotency_key)
Step sendStep    (chunk = 500)  PENDING task 읽어 렌더링 후 SendEmailPort 호출
  reader     LoadPendingTasksPort (status=PENDING)
  processor  템플릿 렌더링(개인화 토큰 치환)
  writer     SendEmailPort 호출 → SENT / FAILED(에러분류) 기록
  · V1은 single-thread. Rate Limit throttle 자리만 마련.
```

### ⑤ retryJob — 재시도 (주기: 수 분 간격)
```
Step retryStep   (chunk = 500)
  reader     LoadRetriableTasksPort (status=FAILED AND next_retry_at <= now AND attempt < max)
  processor  지수 백오프 계산
  writer     재전송 → SENT / FAILED(+attempt, next_retry_at) / DEAD(max 초과·영구오류)
```

상태 전이: `PENDING → SENDING → SENT` / `→ FAILED →(retry)→ SENT | DEAD`

---

## 4. 포트 인터페이스 시그니처

컨텍스트별 inbound(UseCase) / outbound(Port). 헥사고날 — 배치는 UseCase만 호출.

### Source
```
in   CrawlSourcesUseCase      crawlAll() / crawl(sourceId)
in   SeedSourcesUseCase       seed(sources): int            // 심을 카탈로그는 호출자가 결정 (PR #24 리뷰 반영)
in   RenormalizeArticleUrlsUseCase  renormalize(): RenormalizeSummary   // 정규화 규칙 변경분 백필, 멱등 (이슈 #37)
in   ArticleCatalog            findCandidates(from, to): List<ArticleCandidate>   // named interface (D-018·D-030)
                              updateDedupClusters(clusterIdsByArticleId)          // null 값 = 해제 (D-031)
out  ArticleQueryPort          위 두 오퍼레이션의 영속 구현
out  LoadActiveSourcesPort    loadActive(): List<Source>
                              findActiveById(sourceId): Optional<Source>   // crawl(sourceId) 단일 조회 (PR #9 리뷰 반영)
out  FetchFeedPort            fetch(source): List<RawArticle>
out  SaveArticlePort          saveNew(articles): int        // 중복 무시
out  SaveSourcePort           saveNew(sources): int         // url conflict-ignore, 동시 기동 멱등
out  UpdateSourcePort         markCrawled(sourceId, at)     // last_crawled_at 영속 반영 (D-022)
out  ArticleUrlPort           findUrlsAfter(afterId, limit) // 커서 페이징 프로젝션 (본문 미적재)
                              updateNormalizedUrl(id, key): boolean   // 키 점유 시 false (이슈 #37)
```
> **시더는 인바운드 어댑터** (`adapter.in.bootstrap.SourceSeeder`). 기동을 자극으로 받아 `SeedSourcesUseCase`를 호출할 뿐이고, 카탈로그(`SourceSeedData`)도 같은 인바운드에 둔다 — 저장은 `SaveSourcePort`로 내려간다. `content`의 `TopicSeeder`는 아직 out 어댑터에 남아 있다(후속).
> **기동 러너 순서**: `SourceSeeder`(수집의 전제) → `ArticleUrlRenormalizer`(옛 키 정리) → Spring Batch `JobLauncherApplicationRunner`(order 0). 재정규화가 수집보다 뒤로 밀리면 배치가 먼저 새 키로 적재해 같은 기사가 두 행으로 남는다.
> **`ArticleUrlRenormalizer`는 Liquibase 도입 시 마이그레이션으로 옮기고 지운다** — 정규화 규칙이 바뀌면 기존 `normalized_url` 키가 무효해져 재수집 시 중복 행이 생기므로 한 번 훑어 옮겨야 하는데, 마이그레이션 인프라가 아직 없어 기동 러너로 둔 것이다. 멱등이라 매 기동마다 돌아도 결과가 덧나지 않는다 (이슈 #37).
> **Article 애그리거트는 Source 소유 (D-018).** article 테이블 스키마·멱등(UNIQUE normalized_url)의 책임자는 Source.
>
> named interface는 `com.siftnews.source.api` 패키지(`@NamedInterface("article-catalog")`)로 노출하고, Content는 `allowedDependencies = {"common", "source :: article-catalog"}`로 이것만 본다. 경계를 건너는 타입은 `Article` 애그리거트가 아니라 **`ArticleCandidate` record** — 애그리거트를 공개하면 Content가 Source 내부 구조 변경에 묶인다. `category`도 enum이 아니라 문자열로 넘긴다(`Category`는 Source internal). Content 쪽 `adapter/out/source/ArticleCatalogAdapter`가 이를 `CandidateArticle`로 옮긴다 — 이 매핑 한 겹이 두 모듈의 스키마를 떼어 놓는다 (이슈 #29).

### Content (선별)
```
in   NormalizeDedupUseCase        normalizeAndDedup(from, to): NormalizeDedupSummary
in   ScoreArticlesUseCase         scoreTopic(topicId, from, to): ScoreArticlesSummary
in   BuildIssueUseCase            buildIssueForTopic(topicId, runDate): IssueId  // 점수 재계산 없음
in   SeedTopicsUseCase            seed(topics): int             // 심을 카탈로그는 호출자가 결정 (이슈 #39)
out  LoadTopicPort                load(topicId): Optional<Topic>
out  LoadCandidateArticlesPort    loadCandidates(from, to): List<CandidateArticle>  // 윈도우 전체 재계산 (D-031)
out  UpdateArticleClusterPort     updateClusters(clusterIdsByArticleId)  // null 값 = 해제, 벌크 (D-031)
out  SaveArticleScorePort         saveAll(scores)               // (article_id, topic_id) upsert — 재실행 멱등
out  LoadArticleScoresPort        loadByTopic(topicId, computedAtFrom): List<ArticleScore>
out  SaveIssuePort                save(issue): IssueId          // (topic_id, run_date) upsert + 항목 교체
out  SaveTopicPort                saveNew(topics): int          // slug conflict-ignore, 동시 기동 멱등 (이슈 #39)
```
> **토픽 시더도 인바운드 어댑터** (`adapter.in.bootstrap.TopicSeeder`) — Source 쪽과 같은 구조다. 이전에는 `adapter.out.persistence`에 놓인 채 JPA 리포지토리를 직접 부르고 엔티티 매핑까지 들고 있었고, 저장이 "있는지 조회 → 없으면 save"라 **두 인스턴스가 동시에 기동하면 한쪽이 slug 유니크 제약에 걸려 죽는** 창이 있었다 (이슈 #39).
>
> `SaveTopicPort`는 `ON CONFLICT (slug) DO NOTHING` 네이티브 insert다. `include_keywords`·`exclude_keywords`·`keyword_weights`·`source_categories` 네 컬럼은 `@JdbcTypeCode(SqlTypes.JSON)` 매핑을 우회하므로 어댑터가 직렬화한 문자열을 넘기고 리포지토리가 `jsonb`로 캐스팅한다 — Source보다 한 겹 더 들지만 동시 기동 멱등을 같은 방식으로 맞춘다.
> `normalizeAndDedup`은 매 실행이 `[from, to)` 윈도우 내 후보 전체를 재계산해 실행 단위로 클러스터 상태를 통째로 교체한다 — 컷에서 탈락한 기사는 `null`로 해제되며, 이것이 재실행 멱등성의 전제다(D-031).
>
> `scoreTopic`은 **필터 전 후보 전체로 화제성(클러스터 크기)을 집계한 뒤**, 토픽 필터를 통과한 것들만 **클러스터당 대표 한 건**으로 줄여 점수를 매긴다. 순서가 반대면 ① 화제성이 토픽마다 달라지고(토픽 무관 값이어야 한다) ② 대표가 필터에 걸릴 때 통과할 수 있었던 같은 클러스터의 다른 기사까지 잃는다. 대표 선정 규칙은 `RepresentativeRule` 한 곳에 두고 `DedupClusterer`와 공유한다 — 갈라져도 예외가 나지 않고 다른 기사가 실릴 뿐이라 발견이 늦다.
>
> `CandidateArticle`은 `sourceId`·`category`·`dedupClusterId`를 함께 싣는다. 화제성의 클러스터 크기를 별도 집계 쿼리가 아니라 `loadCandidates`가 돌려준 윈도우 안에서 세면, 스코어링 대상이 윈도우를 벗어날 수 없어 **D-032 불변식 (2)가 구조적으로 지켜진다**.

### Subscriber
```
in   ManageSubscriptionUseCase    subscribe(subscriberId, topicId) / unsubscribe(...)
out  LoadTopicSubscribersPort     loadActive(topicId, sendHour): List<Subscriber>   // 시각 필터 (D-019), 스트리밍/페이지
```

### Delivery
```
in   DispatchIssueUseCase           dispatch(issueId): DeliveryJobId      // 스냅샷+전송 트리거
in   SendPendingDeliveriesUseCase   send(deliveryJobId)
in   RetryFailedDeliveriesUseCase   retry()
out  LoadIssuePort / LoadPendingTasksPort / LoadRetriableTasksPort
out  SaveDeliveryJobPort / SaveDeliveryTaskPort
out  SendEmailPort                  send(email): SendResult               // SUCCESS | TRANSIENT | PERMANENT
```

> `SendEmailPort` 구현: `LocalSmtpAdapter`(개발·부하), `SesAdapter`(실증) — 프로파일 전환.

---

## 5. End-to-End 시퀀스 (MVP)

```
1. collectionJob (매시간)          RSS → article 적재
2. selectionTrigger (매일 발행 시각) 전체 active 토픽 → selectionJob 기동 (D-019)
3. selectionJob (토픽별 1회)        article → 스코어링·랭킹 → 당일 issue(SCHEDULED) + issue_item
4. dispatchJob (매시 정각)          당일 이슈 × 현재 시각 선호 구독자 스캔 → 스냅샷 (D-019 pull)
                                   → sendStep: 청크 발송(로컬 SMTP) → SENT/FAILED
5. retryJob (수 분)                FAILED 재시도 → SENT/DEAD
6. (2차) trackingJob               오픈/클릭/바운스 집계
```

---

## 6. 열린 질문 / 다음 단계

> 열린 질문은 해당 이슈 안에서 결정하고 DECISIONS에 기록한다 (TASKS 매핑 기준, 2026-07-05).

- [ ] 소스 RSS URL 실제 확정 (한/영 토픽당 2~3개) → TASKS M2 Topic 이슈
- [ ] 스케줄러 선택: Spring `@Scheduled` (MVP 충분) vs Quartz (분산 시) → TASKS M2 selectionJob 이슈
- [ ] 템플릿 엔진: Thymeleaf vs Mustache (메일 렌더링) → TASKS M3 sendStep 이슈
- [ ] 부하 테스트 시나리오: 구독자 10만 시드 데이터 생성 방법 → TASKS M4 (선호 시각 분포 포함 — D-019)
- [ ] issue 상태 완료 판정: 발송이 24시간에 분산(D-019)될 때 SENDING → SENT 전이 시점 정의 (마지막 시간대 완료? 일 마감?) → M3 발송 스냅샷 이슈
- [x] `Source.markCrawled()` 영속 반영: 전용 포트 `UpdateSourcePort` 신설로 결정 (D-022, 2026-07-13). `source.trust_score` 컬럼은 M1-4 범위에서 제외(도메인 미사용) — M2 스코어링 구현 시 재검토
- [x] → 다음: **프로젝트 스캐폴딩** (`sift-api` Spring 골격 + 모듈/패키지) — 완료, 첫 배치 Job은 TASKS M1
