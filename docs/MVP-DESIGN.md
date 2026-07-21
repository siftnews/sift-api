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

### 소스 시드 (RSS 우선)
- **공통/개발**: Hacker News, 각 사 기술블로그(우아한형제들·카카오·토스 등), Ars Technica
- **AI**: Anthropic/OpenAI/Hugging Face 블로그, Import AI
- **경제**: 한경·매경 RSS, Reuters/Bloomberg(가능 범위)
- > 실제 URL·RSS 여부는 `sift-infra` 작업 시 확정. MVP는 RSS 제공처 위주로 2~3개씩.

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

topic         (id, name, slug, lang_scope,
               include_keywords, exclude_keywords, keyword_weights[json],
               source_categories, recency_half_life_hours, max_items,
               score_threshold, active)
              -- cadence·send_at_hour 제거 (D-019: 매일 고정 + 구독자 선호 시각)

article_score (id, article_id FK, topic_id FK, score, breakdown[json], computed_at)
              · UNIQUE(article_id, topic_id)

subscriber    (id, email, status[ACTIVE|UNSUB|BOUNCED],
               preferred_send_hour,                     -- 0~23, 구독자 선택 수신 시각 (D-019)
               created_at)
              · UNIQUE(email) · INDEX(status, preferred_send_hour)

subscription  (id, subscriber_id FK, topic_id FK, status[ACTIVE|PAUSED], created_at)
              · UNIQUE(subscriber_id, topic_id)

issue         (id, topic_id FK, title, status[DRAFT|SCHEDULED|SENDING|SENT],
               scheduled_at, published_at, created_at)

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

### ① collectionJob — 수집 (주기: 매시간 등)
```
Step collectStep (chunk = 50)
  reader     LoadActiveSourcesPort → 활성 소스 목록
  processor  FetchFeedPort(RSS 파싱) → 신규 기사 추출 + Normalize(URL/본문/lang)
  writer     SaveArticlePort (UNIQUE normalized_url로 중복 무시)
```

### ② selectionTrigger — 일일 발행 트리거 (@Scheduled, 매일 발행 기준 시각 — 예: 06:00, 구현 이슈에서 확정)
```
- 전체 active 토픽 대상 (D-019: 전 토픽 DAILY 고정)
- 각 토픽에 대해 selectionJob 실행 파라미터(topicId, runDate) 투입 → 당일 이슈 생성
```

### ③ selectionJob — 선별 (토픽별, 트리거로 기동)
```
Step normalizeDedupStep         최근 후보 기사 정규화 검증 + dedup_cluster_id 부여
Step scoreStep    (chunk)       토픽 키워드/최신성/화제성/신뢰도 → article_score 저장
Step selectStep                 threshold·랭킹·다양성 → issue(DRAFT→SCHEDULED) + issue_item
  → in: BuildIssueUseCase.buildIssueForTopic(topicId)
```
> MVP는 토픽 단일 처리. 성능 V3에서 토픽 파티셔닝으로 전환.

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
in   (named interface)        후보 기사 조회 — Content의 LoadCandidateArticlesPort가 경유 (D-018)
out  LoadActiveSourcesPort    loadActive(): List<Source>
                              findActiveById(sourceId): Optional<Source>   // crawl(sourceId) 단일 조회 (PR #9 리뷰 반영)
out  FetchFeedPort            fetch(source): List<RawArticle>
out  SaveArticlePort          saveNew(articles): int        // 중복 무시
out  UpdateSourcePort         markCrawled(sourceId, at)     // last_crawled_at 영속 반영 (D-022)
```
> **Article 애그리거트는 Source 소유 (D-018).** article 테이블 스키마·멱등(UNIQUE normalized_url)의 책임자는 Source. named interface 시그니처는 구현 이슈에서 확정.

### Content (선별)
```
in   BuildIssueUseCase            buildIssueForTopic(topicId, runDate): IssueId
out  LoadTopicPort                load(topicId): Topic
out  LoadCandidateArticlesPort    loadCandidates(topic, since): List<Article>
out  SaveArticleScorePort         saveAll(scores)
out  SaveIssuePort                save(issue, items): IssueId
```

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
