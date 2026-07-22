# 구현 보고서 — domain-issue 외부 API 어댑터 HTTP 경로 테스트 (2026-07-22)

| 항목 | 내용 |
|------|------|
| 대상 | `domain-issue` — DART 공시·네이버 뉴스 아웃바운드 어댑터 |
| 상태 | ✅ HTTP 호출 경로(요청 조립·응답 처리) 테스트 추가 완료 |
| 브랜치 | `docs/domain-analysis` |

---

## 1. 배경 — 무엇이 비어 있었나

기존 `domain-issue` 테스트는 **외부 API 클라이언트의 실제 HTTP 호출 경로를 한 번도 타지 않았다.**

- `DartClientTest` → `interpret()`(상태코드 분기)만 순수 검증
- `NaverNewsResponseTest` → 픽스처 JSON의 `toCandidates()` 파싱만 검증
- 수집 서비스 테스트 → 포트 스텁 (게이트웨이 실체 미사용)
- `NaverNewsClient`·`DartClient`는 테스트에서 **인스턴스화조차 되지 않음**

즉 URL 경로, 쿼리 파라미터(`crtfc_key`/`bgn_de`/`end_de`/`query`/`display`/`sort`), 인증
헤더(`X-Naver-Client-Id/Secret`), `ExternalApiClient.execute()` 위의 배선은 검증 공백이었다.
연동 코드·스케줄러 배선(J3-A/J3-B)·설정 키는 존재했으나 **동작이 검증된 적 없는** 상태.

## 2. 구현

`MockWebServer`(목 HTTP 서버)로 두 어댑터의 요청 조립과 응답 처리를 검증. `baseUrl`이 생성자
주입이라 실키·실호출 없이 목 서버로 겨냥 가능하다. CI에서 항상 실행(Testcontainers처럼 스킵되지 않음).

- `DartClientHttpTest` (4건)
  - `list.json`을 키·기간(`bgn_de`/`end_de`)·`page_count=100`으로 호출하는지
  - 정상(`000`) 응답 → 공시 후보 매핑 (stock_code·rcept_no)
  - 데이터없음(`013`) → 빈 목록
  - 오류 상태(`020`, HTTP 200 + 바디) → `IllegalStateException` 전파 (조용한 중단 방지)
- `NaverNewsClientHttpTest` (2건)
  - 쿼리(`query`/`display=10`/`sort=date`)·인증 헤더를 붙이는지
  - 응답 → 뉴스 후보 매핑 (stock_code·url)

의존성: `com.squareup.okhttp3:mockwebserver:4.12.0` (testImplementation).
**버전 명시 이유** — spring-boot 3.4.1 BOM은 okhttp3를 관리하지 않는다. 4.x = classic
`MockResponse().setBody(...)` API.

## 3. 검증 결과

`./gradlew :domain-issue:test` — BUILD SUCCESSFUL. 신규 6건 전부 실행·통과(스킵 0).
모듈 전체 회귀 없음. 기존 스킵 2건(`DisclosureJpaRepositoryTest`·`NewsJpaRepositoryTest`)은
로컬 Docker/docker-java 불일치로 인한 Testcontainers 스킵 — CI(ubuntu)에서 실행(변동 없음).

## 4. 잔여 (BACKLOG 후보)

- **실호출 검증 부재**: KIS의 `scripts/m0-kis-verify`에 해당하는 DART/네이버 실호출 스모크
  스크립트는 아직 없다. 목 테스트는 배선을 검증할 뿐, 실제 API 응답 스키마/키 유효성은 미검증.
  발급 키로 1회 실호출 확인 후 픽스처 갱신 필요.
- DART 페이지네이션(`total_page` 순회) — `DartClient`에 `TODO(M2)`로 남아 있음(단일 페이지).
