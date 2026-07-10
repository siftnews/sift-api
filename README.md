# Sift

외부 뉴스를 자동 수집·선별해 구독자에게 대량 발송하는 **뉴스레터 배치 시스템**의 백엔드.

> 출발 동기: "뉴스를 봐도 내가 원하는 콘텐츠를 식별하기 어렵다." → 수집·선별·발송으로 해결.
> 이 프로젝트의 핵심 서사는 **구독자 규모를 키우며 발송 배치의 병목을 측정하고 개선해 나가는 과정**이다.

## 🤖 에이전트 하네스로 개발합니다

이 레포는 일회성 코드 생성이 아니라 **에이전트 하네스**(규칙·스킬·권한 게이트·루프 프로토콜·외부화된 컨텍스트) 위에서 개발됩니다. 어떻게 운영하는지가 코드만큼 중요한 산출물입니다.

| 위치 | 내용 |
|---|---|
| [siftnews/sift-docs](https://github.com/siftnews/sift-docs) | 하네스 설계(HARNESS)·기획(PLAN)·루프 운영 기록(STATE·BACKLOG·TASKS·DECISIONS) — **커밋 히스토리 자체가 루프를 실제로 운영한 증거** |
| [.claude/settings.json](.claude/settings.json) | 권한 게이트 — 커밋·push·병합 등 되돌리기 어려운 작업은 deny, 사람이 최종 수행 |
| [.claude/skills/](.claude/skills/) | 자작 스킬 — 반복 작업(유스케이스 구현·단위 테스트) 표준화 |
| [docs/](docs/) | 이 레포 종속 설계 문서 — 코드와 같은 PR에서 리뷰·정합 유지 (D-021) |

작업 단위는 **이슈 → 브랜치 → PR → 리뷰 → 병합** 흐름을 따르며, git/GitHub 쓰기는 사람이 직접 수행합니다 (에이전트는 초안·구현·자가검증까지).

## 아키텍처

- **Spring Boot 3.5 / Java 21 / Gradle**
- **Spring Modulith** — 모듈 = 바운디드 컨텍스트: `source` / `content` / `subscriber` / `delivery` / `tracking` (+ `common`), `ApplicationModules.verify()`로 경계 강제
- **모듈 내부는 헥사고날** — `domain` / `application(port.in·out, service)` / `adapter(in, out)`
- **모듈 간 통신은 도메인 이벤트** 우선, 동기 조회는 `port.in` named interface로만
- **Spring Batch Job = 인바운드 어댑터** — 도메인은 배치를 모른다
- 발송 수단은 `SendEmailPort` 뒤로 추상화 (로컬 SMTP ↔ SES 프로파일 전환)

설계 문서: [MVP-DESIGN](docs/MVP-DESIGN.md) (ERD·배치 Job/Step·포트) · [SELECTION](docs/SELECTION.md) (선별 파이프라인) · [EVENTS](docs/EVENTS.md) (도메인 이벤트) — 상위 기획은 [sift-docs의 PLAN](https://github.com/siftnews/sift-docs/blob/main/PLAN.md)

## 시작하기

```bash
./gradlew bootRun    # local 프로파일 → docker compose로 PostgreSQL·mailpit 자동 기동
./gradlew test       # 테스트 (Testcontainers, test 프로파일)
./gradlew build
```

- 로컬 인프라: PostgreSQL(5432), mailpit(SMTP 1025 / UI http://localhost:8025)
- `docker-compose.yaml`이 이 디렉터리에 있으므로 **working directory는 반드시 `sift-api/`**
