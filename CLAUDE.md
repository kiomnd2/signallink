# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트

**시그널링크(signallink)** — 주식 초보자용 시황 "번역" 대시보드의 **백엔드**. 종목이 왜 오르내렸는지를 초보자 언어로 설명하는 것이 핵심. Java 21 · Spring Boot 3.4.1 · Gradle 멀티모듈(Kotlin DSL) · PostgreSQL + Flyway.

기획·구현 계획은 코드가 아니라 문서에 있다. 새 작업 전 반드시 확인:
- `docs/1.이후-구현플랜.md` — 마일스톤(M0~M8)과 현재 위치
- `docs/ARCHITECTURE.md` — 아키텍처 컨벤션(아래 참조)
- `docs/M2-domain-issue-plan.md` 등 도메인별 구현 플랜
- `BACKLOG.md` — 미처리·후속 이슈(새 아이디어/막힌 항목은 전부 여기로, 1차 범위에 추가 금지)

## 빌드 · 테스트 · 실행

```bash
./gradlew build                                   # 전체 빌드 + 테스트 (Gradle 9.0.0 wrapper)
./gradlew :domain-issue:build                     # 단일 모듈
./gradlew :domain-issue:test --tests '*DartListResponseTest'   # 단일 테스트(클래스/패턴)
docker compose up --build                         # 로컬 전체 기동 (postgres + api:8080 + collector:8081)
```

- **테스트 프레임워크**: JUnit 5 + AssertJ. 두 종류가 섞여 있다 — (1) 포트 스텁 기반 순수 단위 테스트, (2) `@DataJpaTest` + **Testcontainers Postgres** 영속 테스트.
- **라이브러리 모듈에서 `@DataJpaTest`를 쓰려면** 테스트 소스에 `@SpringBootApplication` 앵커가 필요하다 (예: `domain-issue`의 `IssueTestApplication`). 도메인 모듈에는 부트 앱이 없기 때문.
- 외부 API 파서는 실호출 대신 `src/test/resources/fixtures/`의 픽스처 JSON으로 검증한다.

## 아키텍처 — 모듈러 모놀리스 + 헥사고날(Level 1)

**Gradle 모듈 = 도메인 경계.** 의존 방향이 **모듈 의존 선언으로 강제**되어 역참조는 컴파일 에러:
```
insight → market · issue · macro   (읽기)
macro   → market
content → (독립)
app-collector / app-api → 모든 domain-* (인바운드 어댑터 위치)
common → (모두가 의존)
```
도메인 간 참조는 **ID(stock_code 등)로만** — 크로스 스키마 FK 없음. 그래서 예를 들어 `issue.disclosure`는 `market.stock`이 비어 있어도 적재된다.

**각 `domain-*` 모듈 내부는 포트&어댑터 패키지 구조**(`docs/ARCHITECTURE.md` 준수). 첫 레퍼런스 구현은 `domain-issue`:
```
io.signallink.<domain>
├─ domain/                엔티티(= 도메인 모델 겸용, @Entity 허용) + 도메인 규칙
├─ application/
│  ├─ port/out/           아웃바운드 포트 인터페이스 (Repository·Gateway·…) — 필수
│  ├─ port/in/            인바운드 포트(유스케이스) — 선택(진입점 하나면 생략)
│  └─ service/            유스케이스 구현(@Service, 포트에만 의존)
└─ adapter/out/{persistence,external}   Spring Data JPA / 외부 API 어댑터(포트 구현)
```
- **Level 1 = 실용형**: JPA 엔티티를 도메인 모델로 겸용(별도 매퍼 없음). 도메인/애플리케이션은 프레임워크 의존 최소, 기술 의존은 `adapter/`에만.
- 인바운드 어댑터: REST 컨트롤러 → `app-api`, 스케줄러 잡 → `app-collector`.
- 네이밍: `*UseCase`(in 포트) / `*Port`(out 포트) / `*Service`(유스케이스) / `*PersistenceAdapter` / `*Client`(외부).

**외부 API 어댑터는 `common.ExternalApiClient`를 상속**(rate limit + 지수 백오프 재시도)하고 게이트웨이 포트를 구현한다. 응답 상태는 "데이터 없음"과 "진짜 오류"를 구분해 후자는 예외를 던진다(조용한 수집 중단 방지). 수집 루프는 **건별 예외 격리**로 한 건 실패가 배치 전체를 죽이지 않게 한다.

- **KIS 호출 한도**: M0 실측 결과 **초당 ~1건**(신규 고객). KIS 클라이언트의 `RateLimiter`는 `permitsPerSecond ≈ 1`로 설정한다.
- 시간은 항상 KST — `common.TimeUtils`(`KST`, `nowKst`, `todayKst`) 사용.
- DB 스키마는 `app-api`의 Flyway `V1__init.sql`이 소유(`market`/`issue`/`macro`/`insight`/`content` 5개 스키마). `ddl-auto: none`.

## 협업 규칙

- **브랜치→PR**: `main` + 작업 브랜치(`feat/`·`fix/`·`docs/`·`chore/`) → PR. CI 통과 필수, 셀프 머지 OK. main 직접 push 금지.
- **커밋 메시지**: gitmoji + 한국어 제목 (예: `✨ ...`, `🐛 ...`). **`Co-Authored-By` 절대 포함하지 않는다**(프로젝트 규칙).
- **CI**(`.github/workflows/ci.yml`): push(main)/PR에서 `gradle build` 실행. `deploy.yml`은 VPS 계약 전까지 비활성.
- **문서 동기화**: 작업을 완료한 뒤 관련 참조 문서(도메인 플랜·`ARCHITECTURE.md`·`BACKLOG.md`·`CLAUDE.md`)를 생성/갱신해 코드와 맞춘다. 문서를 미리 만들어두지 말고, 실제 구현·검증 결과에 맞춰 사후 반영한다.
- **코딩 컨벤션**: 생성자 주입은 Lombok `@RequiredArgsConstructor` + `private final` 필드, getter는 `@Getter`(엔티티·모델)로 — 수동 작성 금지. 단 `@Value`·`super()`가 섞인 특수 생성자(예: `DartClient`·`KisTokenProvider`)는 수동 유지. 외부 API status 코드 등 매직값은 전용 상수 클래스로 분리(예: `DartStatus`).

## 함정 (겪은 것)

- **Gradle 9**: 테스트 런타임에 `junit-platform-launcher`를 자동 주입하지 않는다 → 루트 `build.gradle.kts` subprojects에 `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` 이미 추가됨. 새 모듈도 이 블록을 상속하므로 그대로 두면 됨.
- **`.gitignore`의 `out/`**: IntelliJ 출력 디렉토리를 무시하는데 헥사고날 소스 패키지 `application/port/out`·`adapter/out`과 충돌한다. `!**/src/**/out/` 예외가 있어 추적된다 — 이 예외를 지우면 소스가 커밋에서 조용히 누락된다.
- **Testcontainers 로컬 실행**: 이 개발 머신의 최신 Docker Desktop과 번들 docker-java 버전이 안 맞아 로컬에서 skip될 수 있다(`@Testcontainers(disabledWithoutDocker = true)`). CI(ubuntu)에서는 정상 실행된다.
- **M0 KIS 검증 스크립트**(`scripts/m0-kis-verify/kis_verify.py`): 이 머신 Python이 CA 번들 미보유 → `SSL_CERT_FILE=/etc/ssl/cert.pem python3 ...`로 실행. 키는 gitignore된 `.env`에 `KIS_APPKEY`/`KIS_APPSECRET`(스크립트가 읽는 이름)로 넣는다.
