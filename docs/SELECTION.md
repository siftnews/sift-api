# Sift — 선별 파이프라인 설계 (Selection Pipeline)

> 이 프로젝트의 심장. "원하는 뉴스만 걸러준다"를 실제로 구현하는 부분.
> 상위 기획은 [PLAN.md](https://github.com/siftnews/sift-docs/blob/main/PLAN.md) 참고. · 최종 수정: 2026-07-07

---

## 0. 결정 요약

| 항목 | 결정 | 이유 |
|---|---|---|
| 관심사 단위 | **토픽 구독** | 구독자가 토픽(개발/AI/경제…)을 선택. 단순하면서 "원하는 걸 고른다" 동기 충족 |
| 선별 방식 | **스코어링 + 랭킹** | 5단계 파이프라인 전부 구현. 선별 퀄리티가 눈에 보임 |
| 선별 실행 단위 | **토픽당 1회** | 구독자 수와 무관 → 대량 발송 성능 서사와 충돌 없음 |
| 콘텐츠 동일성 | **같은 토픽 구독자는 같은 이슈 수신** | MVP 단순화. 발송 스냅샷이 깔끔 |

> 확장 경로: (토픽당 이슈) → (구독자가 구독한 여러 토픽을 묶은 **개인 다이제스트**) → (행동 피드백 기반 개인화). 원래 동기는 이 마지막 단계에서 완전히 해결된다.

---

## 1. 토픽 구독 모델

```
Topic {
  id, name, slug,
  includeKeywords[],      // 하나라도 있어야 후보 (없으면 토픽 카테고리만으로 후보)
  excludeKeywords[],      // 있으면 즉시 탈락
  keywordWeights{kw->w},  // 스코어링 가중치 (기본 1.0)
  sourceCategories[],     // 이 토픽이 끌어올 소스 카테고리/태그
  recencyHalfLifeHours,   // 최신성 감쇠 반감기 (예: 24h)
  maxItems,               // 이슈에 담을 최대 기사 수 (예: 10)
  scoreThreshold          // 이 점수 미만은 제외 (품질 하한)
}

Subscription { subscriberId, topicId, status[ACTIVE|PAUSED] }
```

- 구독자는 N개 토픽을 구독한다.
- **이슈(뉴스레터 1회분)는 토픽 단위로 생성**된다. → `Issue.topicId`
- 한 토픽의 이슈는 그 토픽의 모든 ACTIVE 구독자에게 동일 발송.

---

## 2. 파이프라인 5단계

수집된 raw 기사 → 토픽별 선별된 기사 집합(이슈). 가공 배치가 이 순서로 수행한다.

```
1. Normalize  →  2. Dedup  →  3. Filter  →  4. Score  →  5. Rank & Select
   (전역 1회)      (전역 1회)    (토픽별)      (토픽별)       (토픽별)
```

### 1) Normalize — 정규화 (전역 1회)
- URL 정규화: 쿼리스트링/UTM 제거, 호스트 소문자화 → `normalizedUrl`
- 본문 정제: HTML 태그/보일러플레이트 제거, 공백 정리
- 메타 추출: 언어, 본문 길이, 발행시각(없으면 수집시각)
- 컷: 언어 불일치, 본문 최소 길이 미만 → drop

### 2) Dedup — 중복 제거 (전역 1회)
- 1차 키: `normalizedUrl` 완전 일치
- 2차: 제목 유사도 — 토큰 **Jaccard ≥ 임계값**(MVP 기본 0.7, D-030). SimHash는 대규모 전환 시 재검토
- 같은 사건을 보도한 기사들을 하나의 **클러스터**로 묶음 → `dedupClusterId`
  - 대표 기사 1건 선정(최신 또는 신뢰도 높은 소스)
  - **클러스터 크기 = 화제성 신호** (4단계 trendScore 입력)

### 3) Filter — 필터링 (토픽별)
토픽 후보군 추리기. 각 토픽마다:
- `excludeKeywords` 포함 → 탈락
- `sourceCategories` 화이트리스트 매칭
- `includeKeywords` 중 하나 이상 매칭 (정의돼 있을 때)
- 스팸/광고 패턴 컷

### 4) Score — 스코어링 (토픽별)
각 후보 기사에 토픽 관련도 점수 부여. 항목별 0~1 정규화 후 가중합:

```
score = w_kw * keywordScore     // 토픽 키워드 매치. 제목 매치 > 본문 매치
      + w_rc * recencyScore     // exp(-ln2 * ageHours / halfLife)  시간 감쇠
      + w_tr * trendScore       // 정규화된 dedup 클러스터 크기 (화제성)
      + w_src * sourceScore      // 소스별 신뢰도(설정값)

기본 가중치(초안): w_kw 0.5, w_rc 0.2, w_tr 0.2, w_src 0.1
```

- `keywordScore`: Σ(매칭 키워드 weight) × 위치 보정(제목 ×2). 길이로 정규화.
- 점수 산출 근거(breakdown)를 JSON으로 저장 → 디버깅·튜닝·"왜 뽑혔나" 설명에 활용.

**구현 시 확정한 정규화 (이슈 #25)** — 위 초안이 모호하게 남긴 부분을 다음으로 못박았다.

| 항목 | 확정 내용 | 이유 |
|---|---|---|
| `keywordScore` 분모 | **모든 키워드를 제목에서 맞았을 때의 합**(= Σ weight × 2) | "길이로 정규화"를 키워드 목록 길이로 읽었다. 키워드를 많이 등록한 토픽이 불리해지지 않는다 |
| 제목·본문 동시 매치 | 제목 몫만 (2배까지) | 같은 단어를 본문에 도배한 글이 유리해지면 안 된다 |
| `trendScore` | `(clusterSize − 1) / (maxClusterSize − 1)`, 윈도우에 묶인 게 없으면 0 | 크기 1(단독 보도)이 0점 기준. 전원 만점을 주면 항목이 상수가 되어 순위에 기여하지 못한다 |
| `recencyScore` | `publishedAt`이 null이면 0, 미래 시각은 나이 0으로 클램프 | 발행시각 없는 피드는 의도된 설계 — 탈락이 아니라 최신성 이득만 못 본다. 미래 시각을 싣는 피드가 있어 클램프가 없으면 1을 넘는다 |
| `sourceScore` | **중립 상수 1.0** | `source.trust_score`는 M1-4에서 범위 제외되고 "M2 스코어링에서 재검토"로 남아 있다([MVP-DESIGN §2](./MVP-DESIGN.md)). 근거 없는 값을 지어내느니 항목만 배선하고 상수로 둔다 — 실측 근거가 생기면 이 상수만 실제 조회로 바꾸면 된다 |
| 키워드 매칭 | 대소문자 무시 **부분 문자열** | 한국어는 조사가 붙어("금리가") 단어 경계로 자르면 놓친다. 대신 영어에서 `Java`가 `JavaScript`에 걸리는 과매칭을 수용한다 — breakdown 로그로 오탐 빈도를 본 뒤 형태소·단어 경계 도입을 판단 |

> 가중치는 breakdown에 **함께 저장한다** — 나중에 가중치를 튜닝하면 과거 점수를 재현할 수 없게 되는데, 당시 값을 같이 남기면 계속 설명된다.

### 5) Rank & Select — 랭킹 & 선택 (토픽별)
- `scoreThreshold` 미만 제거 (품질 하한)
- score 내림차순 정렬
- **다양성 보장**: 같은 소스/같은 클러스터 쏠림 방지 (단순 MMR — 이미 뽑힌 것과 유사하면 페널티)
- 상위 `maxItems`건 선택 → `Issue` + `IssueItem(rank, score)` 생성

**구현 시 확정 (이슈 #27)**

- **전면 MMR을 넣지 않았다.** 위 다양성 요구 중 *같은 클러스터 쏠림*은 이 단계에 **도달하지 않는다** — Score 단계가 이미 클러스터당 대표 한 건으로 줄인다(`RepresentativeRule`, 이슈 #25). 남은 건 *같은 소스 쏠림*뿐이라 유사도를 다시 도는 대신 **소스별 감점**으로 처리한다. 제목 유사도 MMR은 실측으로 필요가 확인되면 그때 (§6 열린 질문 유지)
- **소스 감점은 곱셈 계수 0.7** — 같은 소스에서 한 건 더 뽑을 때마다 `× 0.7`. 금지가 아니라 감점인 이유는, 그날 좋은 기사가 실제로 한 매체에 몰릴 수 있어 하드 상한을 두면 **빈자리를 낮은 점수로 채우게** 되기 때문이다. 점수 차가 충분히 크면 같은 소스도 이긴다
- **게재 점수는 감점 전 원점수**를 남긴다 — 감점은 순서를 정하는 데만 쓴다
- **동점은 `articleId` 오름차순**으로 깬다 — 순서가 흔들리면 같은 입력에 다른 호가 나온다
- **항목 0건이어도 호는 만든다** — 그날 결과가 없다는 사실도 기록이고, 호가 없으면 "아직 안 돌았다"와 구분되지 않는다
- **점수 로드에 하한이 있다** — `article_score`는 `(article_id, topic_id)`로 upsert되어 과거 기사 점수가 계속 남으므로, `runDate` 자정(UTC) 이후 계산분만 읽는다. 없으면 몇 주 전 기사가 오늘 호에 섞인다

---

## 3. 데이터 모델 (선별 관련)

```
topic         (id, name, slug, include_keywords, exclude_keywords,
               keyword_weights[json], source_categories,
               recency_half_life_hours, max_items, score_threshold)
subscription  (id, subscriber_id, topic_id, status)

article       (id, source_id, url, normalized_url, title, body,
               lang, published_at, category, dedup_cluster_id, created_at)
article_score (id, article_id, topic_id, score, breakdown[json], computed_at)

issue         (id, topic_id, status, scheduled_at, published_at)
issue_item    (id, issue_id, article_id, rank, score)
```

> `article`은 수집 배치가 채우고(**Source 소유, D-018** — Content는 named interface로 조회), `article_score`/`issue`/`issue_item`은 가공(선별) 배치가 채운다.
> `dedup_cluster_id` 갱신은 **Source가 named interface에 노출하는 갱신 오퍼레이션**을 Content가 호출해 수행한다 (D-030 — D-018 꼬리 해소). 후보 조회도 Source named interface 경유.
> **실 배선 완료 (이슈 #29)** — `ArticleCatalog.findCandidates(from, to)` / `updateDedupClusters(map)`. 윈도우는 `article.created_at` 기준 `[from, to)` 반열림이며 **Testcontainers로 경계를 실검증**했다(from 포함·to 미포함). #19~#27까지는 fake 포트로만 확인되던 경로다.
> 이후 발송 배치가 `issue` → 토픽 구독자 → `delivery_task` 스냅샷을 만든다 ([PLAN.md](https://github.com/siftnews/sift-docs/blob/main/PLAN.md) 5장).

---

## 4. 배치 매핑 (Spring Batch)

선별은 **가공 배치(②)** 한 Job. Step 분리:

```
Job: selectionJob
 ├─ Step 1. normalizeStep   (chunk)  raw article → 정규화/컷
 ├─ Step 2. dedupStep                클러스터링 → dedup_cluster_id 부여
 ├─ Step 3. scoreStep       (partition by topic)  토픽×후보 → article_score
 └─ Step 4. selectStep      (by topic)            issue + issue_item 생성
```

> **MVP 구현은 Step 1·2를 `normalizeDedupStep` 하나로 통합** ([MVP-DESIGN §3 ③](./MVP-DESIGN.md) 기준, TASKS M2 "선별 1/3: Normalize + Dedup"과 일치).

- 토픽별 처리는 **파티셔닝 후보**(성능 로드맵 V3와 연결). MVP는 단순 루프로 시작 → 측정 후 파티셔닝 전환.
- Step 간 데이터는 DB 경유(상태 컬럼/중간 테이블)로 멱등성 확보.

---

## 5. 튜닝 & 검증 포인트 (포트폴리오 가치)

- **선별 품질 평가**: 샘플 이슈에 대해 정성 평가 + 가중치 A/B. breakdown 로그로 회귀 분석.
- **임계값/반감기**가 결과에 미치는 영향 실험.
- 향후 **임베딩 기반 유사도/개인화**(LLM·벡터)로 갈 수 있는 추상화 경계 유지 — `Score` 단계를 전략(Strategy)으로 분리해 규칙 기반 ↔ ML 기반 교체 가능하게.

---

## 6. 열린 질문 (다음에 정할 것)

- [ ] 토픽 시드 셋: MVP에 넣을 토픽 3~5개 (예: 개발, AI, 경제…) 와 각 키워드/소스 카테고리
- [ ] Dedup 유사도 방식 확정: SimHash vs Jaccard (성능/정확도 트레이드오프)
- [ ] 다양성(MMR) 도입을 MVP에 넣을지, 2차로 미룰지
- [ ] 가중치 기본값 실측 후 조정
