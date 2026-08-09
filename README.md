# Sift

외부 뉴스를 자동 수집·선별해 구독자에게 대량 발송하는 **뉴스레터 배치 시스템**의 백엔드.

> 출발 동기: "뉴스를 봐도 내가 원하는 콘텐츠를 식별하기 어렵다." → 수집·선별·발송으로 해결.
> 이 프로젝트의 핵심 서사는 **구독자 규모를 키우며 발송 배치의 병목을 측정하고 개선해 나가는 과정**이다.

## 🤖 개발 운영 기록

이 레포의 개발 운영 방식과 설계 결정은 `sift-docs`에 기록됩니다. 과거에는 별도 `sift-harness`를 사용했지만, 현재는 삭제했으며 원격 환경이 필요해질 때 새로 구성합니다.

| 위치 | 내용 |
|---|---|
| [siftnews/sift-docs](https://github.com/siftnews/sift-docs) | 설계·기획·코드 규약·결정·루프 운영 기록의 원본 — 커밋 히스토리 자체가 운영 기록 |

**설계 문서도 `sift-docs`에 있습니다** — ERD·배치 명세([MVP-DESIGN](https://github.com/siftnews/sift-docs/blob/main/references/MVP-DESIGN.md)), 선별 파이프라인([SELECTION](https://github.com/siftnews/sift-docs/blob/main/references/SELECTION.md)), 도메인 이벤트([EVENTS](https://github.com/siftnews/sift-docs/blob/main/references/EVENTS.md)). 이 레포에는 코드만 둡니다 (D-038).

작업 단위는 **이슈 → 브랜치 → PR → 리뷰 → 병합** 흐름을 따릅니다. 과거의 역할 분리·권한 프로파일 운영은 `sift-docs`에 기록으로 남아 있으며 현재 활성 하네스는 없습니다.

이슈·PR 생성과 커밋·push는 **에이전트가 실행**하고(기록은 저자 명의), **병합·리뷰 승인·이슈 close·릴리스는 사람**이 수행합니다.

## 아키텍처

- **Spring Boot 3.5 / Java 21 / Gradle**
- **Spring Modulith** — 모듈 = 바운디드 컨텍스트: `source` / `content` / `subscriber` / `delivery` / `tracking` (+ `common`), `ApplicationModules.verify()`로 경계 강제
- **모듈 내부는 헥사고날** — `domain` / `application(port.in·out, service)` / `adapter(in, out)`
- **모듈 간 통신은 도메인 이벤트** 우선, 동기 조회는 `port.in` named interface로만
- **Spring Batch Job = 인바운드 어댑터** — 도메인은 배치를 모른다
- 발송 수단은 `SendEmailPort` 뒤로 추상화 (로컬 SMTP ↔ SES 프로파일 전환)

상세 설계는 [MVP-DESIGN](https://github.com/siftnews/sift-docs/blob/main/references/MVP-DESIGN.md) (ERD·배치 Job/Step·포트) · [SELECTION](https://github.com/siftnews/sift-docs/blob/main/references/SELECTION.md) (선별 파이프라인) · [EVENTS](https://github.com/siftnews/sift-docs/blob/main/references/EVENTS.md) (도메인 이벤트), 상위 기획은 [PLAN](https://github.com/siftnews/sift-docs/blob/main/references/PLAN.md), 코드 규약은 [coding-conventions](https://github.com/siftnews/sift-docs/blob/main/references/coding-conventions.md)

## 시작하기

```bash
./gradlew bootRun    # local 프로파일 → docker compose로 PostgreSQL·mailpit 자동 기동
./gradlew test       # 테스트 (Testcontainers, test 프로파일)
./gradlew build
```

- 로컬 인프라: PostgreSQL(5432), mailpit(SMTP 1025 / UI http://localhost:8025)
- `docker-compose.yaml`이 이 디렉터리에 있으므로 **working directory는 반드시 `sift-api/`**
