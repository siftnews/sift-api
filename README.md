# Sift

외부 뉴스 소스를 수집하고, 토픽별로 선별한 뒤, 구독자에게 발송하는 뉴스레터 백엔드입니다.

수집·선별·발송 배치의 처리 흐름과 병목을 직접 설계하고 측정하는 것을 목표로 합니다.

## Architecture

Spring Modulith 기반의 모듈러 모놀리스 구조를 사용합니다.

- 모듈은 바운디드 컨텍스트로 분리하고 `ApplicationModules.verify()`로 경계를 검증합니다.
- 모듈 내부는 `domain` / `application` / `adapter`로 나누는 헥사고날 구조를 따릅니다.
- 모듈 간 통신은 도메인 이벤트를 우선합니다.
- 동기 조회가 필요한 경우 공개된 `port.in` named interface만 사용합니다.
- Spring Batch Job은 인바운드 어댑터로 두고, 도메인은 배치 구현을 알지 못하도록 구성합니다.

## Modules

| Module | Responsibility |
|---|---|
| `source` | RSS/Atom 소스 관리와 기사 수집 |
| `content` | 기사 정규화·중복 제거·필터링·스코어링·이슈 구성 |
| `subscriber` | 구독자와 토픽 구독 관리 |
| `delivery` | 발송 작업 생성·메일 발송·재시도 |
| `tracking` | 오픈·클릭·바운스 추적 (2차 범위) |
| `common` | 공통 엔티티와 기반 설정 |

## Batch Pipeline

```text
External News Sources
        │
        ▼
collectionJob ── 기사 수집
        │
        ▼
selectionJob ── 정규화 → 중복 제거 → 필터링 → 스코어링 → 랭킹/선택
        │
        ▼
Newsletter Issue + Subscribers
        │
        ▼
dispatchJob ── 발송 작업 생성 및 메일 전송
        │
        ▼
retryJob ── 실패한 발송 작업 재시도
```

| Job | Responsibility | Default Trigger |
|---|---|---|
| `collectionJob` | 활성 뉴스 소스에서 기사 수집 | 매시 10분, `Asia/Seoul` |
| `selectionJob` | 후보 기사 선별 및 뉴스레터 이슈 구성 | 매일 00:00, `Asia/Seoul` |
| `dispatchJob` | 이슈 스냅샷 생성 및 메일 발송 | 매시 정각 |
| `retryJob` | 실패한 발송 작업 재처리 | 5분 간격 |

## Technology Stack

| Area | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.5.16, Gradle |
| Architecture | Spring Modulith 1.4.12, Hexagonal Architecture |
| Batch | Spring Batch |
| Persistence | Spring Data JPA, PostgreSQL 16, Liquibase |
| Email | Spring Mail, Mailpit (local) |
| Observability | Spring Boot Actuator, Prometheus |
| Test | JUnit, Spring Modulith Test, Testcontainers |

## Key Engineering Points

### Data and Boundary

모듈별 책임과 의존 방향을 명시하고, 모듈 외부에는 필요한 인터페이스만 공개합니다.

### Delivery Reliability

메일 발송 수단은 `SendEmailPort` 뒤로 추상화합니다. 로컬에서는 Mailpit을 사용하고, 발송 실패는 재시도 Job으로 처리합니다.

### Performance Verification

배치 전체 시간이 아닌 Job·Step별 처리 시간과 처리량을 측정하여 병목을 확인합니다. 동일한 workload를 다시 실행해 개선 전후를 비교합니다.

## Getting Started

```bash
./gradlew bootRun    # local 프로파일과 PostgreSQL·Mailpit 자동 기동
./gradlew test       # Testcontainers 기반 테스트
./gradlew build
```

로컬 실행 시 `docker-compose.yaml`을 통해 다음 인프라가 기동됩니다.

- PostgreSQL: `localhost:5432` (`sift` / `sift` / `sift`)
- Mailpit SMTP: `localhost:1025`
- Mailpit UI: <http://localhost:8025>

## Documentation

설계·운영 문서는 [sift-docs](https://github.com/siftnews/sift-docs)에서 관리합니다.

- [MVP 설계](https://github.com/siftnews/sift-docs/blob/main/references/mvp-design.md)
- [선별 파이프라인](https://github.com/siftnews/sift-docs/blob/main/references/selection.md)
- [이벤트 명세](https://github.com/siftnews/sift-docs/blob/main/references/events.md)
- [관측 기준](https://github.com/siftnews/sift-docs/blob/main/references/observability.md)
- [코드 규약](https://github.com/siftnews/sift-docs/blob/main/references/coding-conventions.md)
