# M2 — domain-issue 구현 플랜 (공시·뉴스 수집)

| 항목 | 내용 |
|------|------|
| 작성일 | 2026-07-07 |
| 기준 | 구현플랜 M2 #4·#5 · 기획확정서 v2.5 · 상세스펙 2장(스키마) · `docs/ARCHITECTURE.md` |
| 범위 | `domain-issue`: DART 신규 공시 폴링 + 카테고리 분류(J3-A), 네이버 뉴스 수집(J3-B) |
| 아키텍처 | **헥사고날 Level 1(실용)** — 이 프로젝트의 첫 레퍼런스 구현. `docs/ARCHITECTURE.md` 준수 |
| 핵심 | **KIS와 완전 독립** — DART·네이버 키만 있으면 M0(KIS) 전에 착수·완료 가능 |
| 원칙 | 실응답 JSON 픽스처로 파서 단위 테스트 + "하루 돌려보고 row/null 체크" (완벽 금지, Exit 통과 목표) |

---

## 0. 왜 이걸 먼저 하는가

M2 구현 순서는 원래 market(1~3, KIS 필요) → issue(4~5)지만, **KIS 실전 키가 아직 없어** market 파트가 막혀 있다. issue는:

- 스키마(`issue.disclosure`, `issue.news`)가 **`market.stock`에 FK가 없고 `stock_code`만 보관** → market이 비어도 적재됨
- OpenDART·네이버 키가 **즉시 발급**(무료) → 대기 없음
- 도메인 경계상 **자기 테이블만 write**, 다른 도메인은 ID(stock_code)로만 참조

→ KIS를 기다리지 않고 크리티컬 패스를 진행할 수 있는 유일한 M2 파트.

---

## 1. 선행 조건 (키 발급 — 즉시)

- [ ] **OpenDART** API 키 — https://opendart.fss.or.kr (무료, 즉시)
- [ ] **네이버 검색** API `Client ID/Secret` — https://developers.naver.com (일 25,000건 무료, 즉시)
- `.env.example`에 슬롯 이미 존재: `DART_API_KEY`, `NAVER_CLIENT_ID`, `NAVER_CLIENT_SECRET` → 로컬 `.env`에 값만 채우면 됨

> ⚠️ 키 없이도 **엔티티·리포지토리·파서·카테고리 룰**까지는 픽스처 기반으로 구현·테스트 가능. 실호출(수집기·스케줄러)만 키 대기.

---

## 2. 대상 스키마 (기존 `V1__init.sql` — 변경 없음)

```
issue.disclosure (id, stock_code, rcept_no UNIQUE, title, category, disclosed_at, dart_url)
  idx: (stock_code, disclosed_at)
issue.news       (id, stock_code, title, summary, url, source, published_at)
  UNIQUE (stock_code, url)   ← 중복 제거는 DB 제약이 1차 방어
  idx: (stock_code, published_at)
```

`ddl-auto: none` (Flyway 소유) · `OffsetDateTime` ↔ `timestamptz` · 스키마명 `issue` 명시 필수.

---

## 3. 구성 요소 (헥사고날 Level 1)

패키지 배치는 `docs/ARCHITECTURE.md` §2 표준을 따른다. `port/in`(유스케이스 인터페이스)은 수집 잡이 유일 진입점이라 **생략**하고 `@Service`를 인바운드 어댑터가 직접 호출(§5 실용 규칙). **`port/out`은 필수** — 여기서 워치리스트 교체·외부 API 스텁 이득이 나온다.

```
io.signallink.issue
├─ domain/                     Disclosure, News            (@Entity 겸 도메인)
│                              DisclosureCategoryClassifier (제목 키워드 룰 — 도메인 규칙)
├─ application/
│  ├─ port/out/                DisclosureRepositoryPort, NewsRepositoryPort
│  │                           DartGatewayPort, NaverNewsGatewayPort, WatchlistPort
│  └─ service/                 DisclosureCollectService, NewsCollectService
│                              (@Service, port/out에만 의존)
└─ adapter/out/
   ├─ persistence/             DisclosureJpaRepository, NewsJpaRepository (Spring Data)
   │                           DisclosurePersistenceAdapter, NewsPersistenceAdapter (impl *RepositoryPort)
   │                           SeedWatchlistAdapter (impl WatchlistPort — 시드 CSV, §8)
   └─ external/                DartClient (impl DartGatewayPort), NaverNewsClient (impl NaverNewsGatewayPort)
                               DartListResponse, NaverNewsResponse (응답 DTO, record)
```

인바운드 어댑터(J3)는 **app-collector**에 위치: `io.signallink.collector.job.DisclosureJob`·`NewsJob` (`@Scheduled` → `*CollectService` 호출, 기존 `HeartbeatJob` 제거).
설정: `IssueApiProperties`(`@ConfigurationProperties("signallink.issue")`) — 키 바인딩.

| 포트 | 아웃바운드 의존 격리 대상 | 이득 |
|------|--------------------------|------|
| `*RepositoryPort` | DB(JPA) | 어댑터 교체·서비스 단위 테스트 시 인메모리 스텁 |
| `DartGatewayPort` / `NaverNewsGatewayPort` | 외부 API | 픽스처 스텁으로 서비스 테스트 (실호출 없이) |
| `WatchlistPort` | 뉴스 대상 종목 소스 | **시드 CSV → 나중에 특징주(market)로 어댑터만 교체** (§8) |

### 빌드 의존성 추가 (`domain-issue/build.gradle.kts`)
현재 `common` + `spring-boot-starter-data-jpa`만 있음. `ExternalApiClient`의 `RestClient`(spring-web)는 common에서 `implementation`이라 **전이되지 않음** → 추가 필요:
```kotlin
implementation("org.springframework:spring-web")          // RestClient 서브클래스 컴파일용
implementation("org.springframework.boot:spring-boot-starter-json") // Jackson (JSON 파싱)
```

---

## 4. DART 공시 폴링 (J3-A) 상세

**엔드포인트**: `GET https://opendart.fss.or.kr/api/list.json`
주요 파라미터: `crtfc_key`, `bgn_de`/`end_de`(YYYYMMDD), `page_no`, `page_count`(최대 100), (선택 `pblntf_ty` 공시유형).

> ⚠️ **정책③ 조회 100건 제한**: 현재 `DartClient`는 단일 페이지(`page_count=100`, 코드에 TODO)라 하루 공시 100건 초과 시 유실 가능. `total_page` 순회(페이지네이션) 필요 — BACKLOG "KIS API 정책 준수 감사"(실적 시즌 등 대량 공시일 대비).

**설계 포인트** (플랜 반영 필수):
1. **corp_code vs stock_code**: DART 개별 조회는 8자리 `corp_code`(고유번호)를 쓰지만, **날짜범위 폴링(corp_code 생략)** 하면 응답에 `stock_code`(6자리)가 함께 온다 → **시장 전체 최근 공시를 폴링하고 상장사(stock_code 있음)만 남기는 방식**으로 corp_code 매핑 없이 진행. (corp_code↔stock_code 매핑 `corpCode.xml`은 M3 재무 추세에서 필요 → 그때 도입)
2. **신규 판별**: `rcept_no`가 UNIQUE → 이미 있으면 skip. `existsByRceptNo` 또는 저장 시 제약 위반 무시. 폴링 주기는 최근 N분 겹치게(멱등).
3. **category 분류** (`report_nm` 키워드 룰, 예시 — 스펙 확정 시 확장):
   | 키워드 | category |
   |--------|----------|
   | 단일판매·공급계약, 수주 | `수주계약` |
   | 잠정실적, 영업(잠정) | `실적` |
   | 유상증자, 전환사채, 신주인수권 | `자금조달` |
   | 소송, 분쟁 | `소송` |
   | 최대주주, 주식등의 대량보유 | `지분변동` |
   | (매칭 없음) | `기타` |
4. **필드 매핑**: `report_nm`→title, `rcept_dt`(YYYYMMDD)→disclosed_at(KST, `TimeUtils`), `dart_url` = `https://dart.fss.or.kr/dsaf001/main.do?rcpNo={rcept_no}`.
5. `status.rt_cd == "000"` 아닐 때(013 데이터없음 등) 정상 분기 처리.

---

## 5. 네이버 뉴스 수집 (J3-B) 상세

**엔드포인트**: `GET https://openapi.naver.com/v1/search/news.json?query={종목명}&display=10&sort=date`
헤더: `X-Naver-Client-Id`, `X-Naver-Client-Secret`.

**설계 포인트**:
1. **쿼리 대상(워치리스트) — 이 도메인의 유일한 외부 의존**: 뉴스는 "종목명"으로 조회하는데, 정식 워치리스트(특징주 40종목)는 J2(M3, KIS 필요)에서 나온다. → **개발·테스트 단계는 시드 워치리스트(잘 알려진 20~30종목 `code,name` CSV/시드)로 구동**하고, `market.stock`이 채워지면 소스만 교체. (아래 §8 결정사항)
2. **중복 제거**: `UNIQUE(stock_code, url)` 1차 방어 + 저장 전 `existsByStockCodeAndUrl` 체크. `link`(네이버) 기준.
3. **필드 매핑**: `title`/`description`의 **HTML 태그(`<b>`)·엔티티(`&quot;` 등) 제거** 후 title/summary, `pubDate`(RFC1123 `+0900`)→published_at, `source`는 원문 도메인 또는 "naver".
4. **예산**: 일 25,000건. 30종목 × 30분 배치 × ~20시간 = 약 1,200콜/일 → 여유. `RateLimiter`로 초당 간격만 완만하게.

---

## 6. 스케줄러 (J3) — app-collector

- 기존 `HeartbeatJob` **제거**, `io.signallink.collector.job`에 `DisclosureJob`·`NewsJob` 추가
- `@Scheduled(cron = ...)` KST 기준 (`TimeUtils`) — 공시·뉴스는 30분 주기(장중), 야간 저빈도
- **종목 단위 예외 격리**: 한 종목 실패가 배치 전체를 죽이지 않게 try/catch + `DiscordNotifier`로 실패 알림
- 휴장일 캘린더는 M2 #6 항목 — 여기선 일단 상시 폴링, 캘린더는 후속

---

## 7. 테스트 전략 (스펙 §M2 원칙)

- [ ] **파서 단위 테스트**: DART·네이버 실응답 JSON을 `src/test/resources/fixtures/`에 저장 → DTO 파싱·필드 매핑 검증 (외부 호출 없이)
- [ ] **카테고리 룰 테스트**: 대표 공시 제목 10여 개 → 기대 category 매핑
- [ ] **중복 제거 테스트**: 같은 rcept_no/url 두 번 저장 → 1건만 (H2 or @DataJpaTest + Testcontainers postgres 중 택1; 스키마 있으므로 Testcontainers 권장)
- [ ] **드라이런 스크립트**: 키 넣고 하루 돌린 뒤 `disclosure`/`news` row 수·null 비율 확인 (통합 자동화 대신 실용 검증)

---

## 8. 결정 사항 (확정)

**네이버 뉴스 워치리스트 소스** → ✅ **시드 CSV 20~30종목으로 개발/테스트, 나중에 특징주로 교체** (2026-07-07 확정)
- 시드 파일: `domain-issue/src/main/resources/seed/watchlist.csv` (`stock_code,name` — 삼성전자·SK하이닉스 등 대형주 위주 20~30)
- `NewsCollector`는 워치리스트를 인터페이스로 주입받아, J2(특징주, M3) 완성 시 소스만 `market.stock`/특징주로 교체 — 수집기 코드 변경 없이 전환
- 지금 시점에 뉴스 파이프라인 전체를 end-to-end 검증 가능 (KIS 무관)

DART는 워치리스트 무관(시장 전체 폴링 후 stock_code 필터)이라 결정 불필요.

---

## 9. 구현 순서 (체크리스트)

> **진행 현황 (2026-07-08)**: 공시(DART)·뉴스(네이버) 세로 슬라이스 **완료**. 설정 바인딩(9)·스케줄러 배선(10, `DisclosureJob`·`NewsJob`) 완료. 남음: 실호출 dry-run(11, 키 채우고 하루 돌려 row/null 검증).

1. [x] `domain-issue/build.gradle.kts`에 spring-web + starter-json 추가, 빌드 확인
2. [x] `domain/`: 엔티티 `Disclosure` (@Entity 겸 도메인) + `application/port/out`: `DisclosureRepositoryPort` *(News 엔티티는 뉴스 슬라이스에서)*
3. [x] `adapter/out/persistence`: `DisclosureJpaRepository`(+exists) + `DisclosurePersistenceAdapter`(impl 포트) → `@DataJpaTest`(Testcontainers) 저장/중복 제약 테스트
4. [x] `domain/`: `DisclosureCategoryClassifier` + 룰 단위 테스트
5. [x] `application/port/out`: `DartGatewayPort` + `adapter/out/external`: `DartClient`·`DartListResponse` + 픽스처 파서 테스트 (+ 상태코드 해석 `interpret()`)
6. [x] `application/service`: `DisclosureCollectService` (포트 주입: DartGateway→필터→RepositoryPort 저장) + 스텁 포트 서비스 단위 테스트 (+ 건별 예외 격리)
7. [x] `application/port/out`: `NaverNewsGatewayPort`·`WatchlistPort` + `adapter/out/external`: `NaverNewsClient`·`NaverNewsResponse`(태그 제거) + 픽스처 파서 테스트
8. [x] `adapter/out/persistence`: `SeedWatchlistAdapter`(시드 CSV, §8) + `application/service`: `NewsCollectService`(WatchlistPort 주입 구동)
9. [x] application.yml `signallink.issue.*` 바인딩 (`@Value` 직접 주입 — 클라이언트별 `base-url`/키/`permits-per-second`)
10. [x] app-collector 인바운드 어댑터 `DisclosureJob`·`NewsJob` (`@Scheduled` → `*CollectService` 호출) — 장중 30분 주기(공시 `0/30`, 뉴스 `15/30` 엇갈림), KST
11. [ ] 키 채우고 로컬 드라이런 → row/null 검증

**의존 순**: 1→2→(3·4 병행)→5→6→7→8→9→10→11. 키 없으면 1~7까지(도메인·포트·어댑터·파서·룰·서비스 단위 테스트) 먼저 — 포트 스텁 덕에 실호출 없이 서비스까지 검증 가능.

---

## 10. Exit 조건

- 평일 하루 무인 방치 후 `issue.disclosure`·`issue.news`에 당일 데이터 정상 적재
- 재폴링해도 중복 row 안 생김(rcept_no / (stock_code,url) 제약 동작 확인)
- 파서·카테고리·중복 단위 테스트 그린 + CI 통과

> 이 문서는 domain-issue의 PDCA Plan에 해당. 구현 중 스펙 변경 발견 시 `BACKLOG.md` "스펙 반영 대기"에 기록.
