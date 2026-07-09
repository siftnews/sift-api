# 이벤트 명세

> 컨텍스트 간 도메인 이벤트의 계약 문서. 컨텍스트 맵(siftnews_설계 자료.drawio)과 정합 유지.
> MVP 이벤트: `DeliveryJobCompleted` · 2차: `BounceDetected`
> (검토 후 제외된 이벤트의 이력은 [DECISIONS](https://github.com/siftnews/sift-docs/blob/main/DECISIONS.md) 참조 — 예: IssueScheduled, D-019)
>
> 공통 구현 규약: 발행 = `ApplicationEventPublisher` (상태 전이와 같은 트랜잭션 안에서 publish),
> 구독 = `@ApplicationModuleListener` (커밋 후 + 비동기 + Event Publication Registry 영속화 → **at-least-once**).
> at-least-once이므로 **모든 구독자는 멱등이어야 한다.**

---

## DeliveryJobCompleted

### 목적
- 발송 한 회(delivery_job)가 종료되면 Tracking이 발송 결과 집계를 기록하도록 알린다.

### Publisher / Subscriber
- 발행: delivery (dispatchJob sendStep 종료 처리)
- 구독: tracking

### 발생 시점
- delivery_job이 SENDING → DONE으로 전이된 **트랜잭션이 커밋된 후** 발행된다.
- ⚠️ DONE의 정의(1차 발송 종료 시점 vs 재시도 포함 전 task 종결 시점)는 M3 발송 스냅샷 이슈에서 확정
  — 확정 시 이 항목 갱신.

### Payload

```java
public record DeliveryJobCompleted(
        Long deliveryJobId,
        Long issueId,
        int totalCount,
        int sentCount,
        int failedCount,
        Instant completedAt
) {
}
```

- **job 단위 요약만 담는다** — task(건별) 단위로 발행하면 10만 구독자 = 이벤트 10만 개가 되어
  발송 성능을 갉아먹는다. 건별 결과의 진실원천은 `delivery_task` 테이블이며, Tracking이
  상세가 필요하면 조회한다. (컨텍스트 맵 라벨과 일치)

### 구독자 행동
- Tracking: 발송 회차 집계 레코드 생성 (성공/실패 수, 소요 정보). 2차에서 오픈/클릭 집계와 조인되는 기준점.

### 보장 사항
- at-least-once — Event Publication Registry가 발행을 영속화, 리스너 완료 전 장애 시 재전달.
- 구독은 `@ApplicationModuleListener` (커밋 후 + 비동기 + Registry 기록).

### 실패 처리
- Tracking 리스너 실패 시: publication이 미완료로 남아 재처리 대상이 된다.
- **멱등성**: 집계 레코드를 `delivery_job_id` UNIQUE로 저장(upsert) → 중복 수신해도 집계 중복 없음.

### 범위
- MVP

### 비고
- 발송 실패가 있어도(failedCount > 0) 이벤트는 발행된다 — "완료"는 성공이 아니라 **회차 종료**를 뜻한다.
  실패 건의 후속 처리는 retryJob(Delivery 내부)의 몫이며 이 이벤트와 무관.

---

## BounceDetected

### 목적
- 반송(bounce)이 감지된 구독자를 Subscriber가 발송 대상에서 제외하도록 알린다 (발신자 평판 보호).

### Publisher / Subscriber
- 발행: tracking (반송 통지 수집·판정)
- 구독: subscriber

### 발생 시점
- Tracking이 반송 통지(DSN / SES SNS 알림)를 수집해 판정을 마친 후 발행된다.
  - HARD (5.x.x — 주소 없음 등 영구 오류): 즉시 발행
  - SOFT (4.x.x — 메일함 초과 등 일시 오류): 반복 임계(N회) 도달 시 발행 — N은 2차 이슈에서 확정

### Payload

```java
public record BounceDetected(
        Long subscriberId,
        BounceType bounceType,   // HARD | SOFT
        String reason,           // SMTP 상태 코드/사유 (예: "550 5.1.1 User unknown")
        Instant detectedAt
) {
}
```

- email이 아닌 `subscriberId` 참조 — ID 참조 원칙 + 이벤트에 개인정보 비포함.
  (반송 통지 ↔ 구독자 매칭은 delivery_task 경유, Tracking 내부 책임)

### 구독자 행동
- Subscriber: `status` ACTIVE → BOUNCED 전환 → 이후 스냅샷 생성 시 자동 제외 (Delivery는 ACTIVE만 로드).

### 보장 사항
- at-least-once — 공통 규약과 동일.

### 실패 처리
- Subscriber 리스너 실패 시: publication 미완료 → 재처리.
- **멱등성**: 이미 BOUNCED인 구독자면 무시 (상태 전이 자체가 멱등).

### 범위
- **2차** — MVP는 mailpit(로컬 SMTP)이라 실제 반송이 발생하지 않는다. SES 실증 단계에서 구현.

### 비고
- `UNSUB`(본인 의사 해지)와 `BOUNCED`(기술적 전달 불가)는 원인이 다르므로 구분 유지 — 복구 경로가 다르다.
- 발송 시점 동기 실패(PERMANENT → task DEAD)는 Delivery의 에러 분류 소관으로 이 이벤트와 별개.
  DEAD 반복 구독자를 상태에 반영할지(`PermanentFailureDetected` 유사 이벤트)는 2차 바운스 이슈에서 함께 결정.

---

