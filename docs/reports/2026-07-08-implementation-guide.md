# 구현 상세 가이드 — 어떻게 만들었나 (2026-07-08)

domain-issue·domain-market를 어떤 구조·패턴·결정으로 구현했는지 코드 수준으로 정리한 문서. 새 도메인(M3~)도 이 패턴을 따른다.

---

## 1. 전체 골격 — 모듈러 모놀리스 + 헥사고날 Level 1

Gradle 멀티모듈이 **도메인 경계 = 컴파일 경계**를 강제한다.
```
common / domain-{market,issue,macro,insight,content} / app-{collector,api}
의존: insight → market·issue·macro,  macro → market,  content 독립,  app-* → 모든 domain-*
```
역참조는 컴파일 에러(빌드에서 검증). 도메인 간 데이터는 **ID(stock_code 등)로만** 참조하고 크로스 스키마 FK를 걸지 않는다 → 그래서 `issue.disclosure`는 `market.stock`이 비어도 적재된다.

각 도메인 모듈 내부는 포트&어댑터:
```
io.signallink.<domain>
├─ domain/                엔티티(= 도메인 모델 겸용, @Entity 허용) + 순수 계산·규칙
├─ application/
│  ├─ port/out/           아웃바운드 포트 인터페이스 (Repository·Gateway·Query) — 필수
│  └─ service/            유스케이스(@Service, 포트에만 의존)
└─ adapter/out/{persistence, external}   Spring Data JPA / 외부 API 어댑터(포트 구현)
```
- **Level 1 절충**: JPA 엔티티를 도메인 모델로 겸용(별도 매퍼 없음). 도메인/애플리케이션은 프레임워크 의존 최소, 기술 의존은 `adapter/`에만.
- 인바운드 어댑터(스케줄러 잡)는 `app-collector`에 두고 도메인 서비스를 호출.

---

## 2. 외부 API 연동 패턴

**공통 베이스 `common.ExternalApiClient`** — rate limit + 지수 백오프 재시도를 한곳에.
```java
class DartClient extends ExternalApiClient implements DartGatewayPort {
    DartClient(...) { super(baseUrl, new RateLimiter(pps), 3, 500); }
    List<DisclosureCandidate> fetchDisclosures(...) {
        var res = execute("dart.list", () -> client().get().uri(...).retrieve().body(DartListResponse.class));
        return interpret(res);
    }
}
```
- **RateLimiter**: 초당 blocking 스로틀. KIS는 M0 실측값 `RateLimiter(1.0)`(초당 1건), DART/네이버는 각 한도.
- **KIS 토큰(`KisTokenProvider`)**: `tokenP` 재발급 제한 대응 — 토큰을 **DB(market.kis_token)에 저장하고 만료 10분 전이면 재사용**, 임박 시에만 재발급. 모든 KIS 호출 헤더에 Bearer + appkey/appsecret/tr_id 부착.
- **인증 헤더**: `client().get().uri(...).headers(h -> { h.setBearerAuth(token); h.add("tr_id", ...); ... })`.

---

## 3. 수집 파이프라인 패턴 (도메인마다 동일)

```
게이트웨이 포트 → 응답 DTO(파서) → 후보 record → 유스케이스(필터·멱등·예외격리) → 영속 어댑터
```
- **응답 DTO = 파서**: 외부 JSON을 record로 받고 `toXxx()`에서 **도메인 경계 타입(후보)로 변환**. snake_case는 `@JsonProperty`, 결함 항목은 로그 후 skip(한 건이 페이지 전체를 유실시키지 않음).
- **유스케이스**: 후보를 순회하며 (1) 무효 필터, (2) **중복/멱등**(`existsByRceptNo`, `(stock,date)` 등), (3) 저장. **건별 try/catch로 예외 격리** — 한 종목 실패가 배치 전체를 죽이지 않음.
```java
for (Candidate c : gateway.fetch(...)) {
    if (skip(c)) continue;
    try { if (!repo.exists(c.key())) { repo.save(build(c)); saved++; } }
    catch (DataIntegrityViolationException e) { /* 멱등 */ }
    catch (RuntimeException e) { log.warn("건너뜀", e); }
}
```

---

## 4. 계산 로직 — 순수 도메인 클래스

외부 응답에 **없는 값은 직접 계산**(실검증으로 발견). 전부 프레임워크 비의존 → 단독 단위 테스트.
- **등락률**(일봉): `prdy_vrss`(전일대비)/전일종가. 지수: 직전 행 종가 대비.
- **vol_ratio_20d**: 당일 거래량 / 직전 20영업일 평균 — 수집 시 KIS 시계열에서 **인메모리 계산**(DB 조회 포트 안 늘림).
- **beta_60d**(`BetaCalculator`): 종목·지수 종가를 공통 거래일로 정렬 → 일수익률 → `Cov/Var`. 20개 미만·분산0이면 null.

---

## 5. 영속·upsert 전략 (상황별로 다르게)

| 상황 | 전략 | 예 |
|------|------|-----|
| 새 데이터만 추가 | **skip-existing**(`existsBy...`) | daily_price, disclosure, news |
| 잠정→확정 덮어쓰기 | **find-then-update**(surrogate id + unique) | investor_flow |
| 자연 PK 갱신 | **save=merge**(id 지정 엔티티) | stock, stock_beta |
| 복합 PK | `@IdClass` + Lombok Pk | index_price(index_code,trade_date) |

```java
// investor_flow upsert (잠정→확정)
jpa.findByStockCodeAndTradeDateAndInvestorType(...).ifPresentOrElse(
    e -> { e.update(qty, amt, isFinal); jpa.save(e); },   // 덮어쓰기
    () -> jpa.save(flow));                                  // 삽입
```

---

## 6. 스케줄러·백필

- **`MarketCollectJob`** `@Scheduled(cron "평일 16:30 KST")` — cron의 `MON-FRI`가 곧 휴장(주말) 가드, 공휴일은 멱등 수집이 흡수(신규 0건).
- **`StockBetaJob`** 토 06:00 주간.
- **`BackfillRunner`** `@ConditionalOnProperty(backfill.enabled)` — 수동 1회. 2년을 **100일 창으로 분할**(KIS 조회 100건 제한=정책③), 종목 데이터 있으면 스킵(재개).

---

## 7. 테스트 전략

3층으로 나눠 **로컬에서 빠르고 CI에서 완전하게**.
- **순수 단위**: 계산기(등락률/β/vol_ratio)·분류기 — 프레임워크 없이 값 검증. (예: 종목=지수 2배 → β=2.000)
- **유스케이스**: **포트 스텁(람다/인메모리)** 로 서비스 로직만 — 외부 API/DB 없이 멱등·예외격리·is_final 검증.
- **영속**: `@DataJpaTest` + **Testcontainers Postgres**(실제 스키마·유니크·복합키·upsert). `@Testcontainers(disabledWithoutDocker=true)`로 **Docker 없는 로컬은 skip, CI(ubuntu)는 실행**. 라이브러리 모듈이라 `@SpringBootApplication` 테스트 앵커(`*TestApplication`) 필요.
- **파서**: 실응답 픽스처(JSON/.mst)를 저장해 필드·매핑 검증(추측 금지 — KIS 실호출로 필드 확인 후 픽스처화).

---

## 8. 컨벤션 (CLAUDE.md)

- 생성자 주입 = Lombok `@RequiredArgsConstructor` + `private final`, getter = `@Getter`. (`@Value`·`super()` 섞인 생성자는 수동 예외.)
- 외부 status 코드 등 매직값 → 상수 클래스(`DartStatus`).
- 브랜치→PR(gitmoji 한국어 커밋, Co-Authored-By 금지), CI 필수.
- **작업마다 구현 보고서**(`docs/reports/`) + 참조 문서 사후 동기화.

---

## 9. 겪은 이슈·교훈

- **KIS 응답에 등락률 없음** → 계산으로 해결(실검증의 가치 — 추측했으면 틀렸을 부분).
- **`.env` 값 따옴표** → 수동 파서에서 `EGW00103 유효하지 않은 AppKey` 유발. 셸 `source`는 벗기지만 일부 로더는 아님 → 따옴표 제거. ("발급 제한/차단"은 오진이었음.)
- **PR 머지 시 후속 커밋 누락**(#7) → 커밋 SHA가 살아있어 cherry-pick으로 복구.
- **토큰 캐시 삭제 금지**(재발급 제한). 앱은 DB 캐시라 자동 방지 — 수동 스크립트만 주의.
- **포트에 메서드 추가 = 스텁 깨짐** → 조회는 별도 QueryPort로 분리하거나 인메모리 계산으로 회피.
- **`out/` gitignore**가 헥사고날 `port/out`·`adapter/out` 소스를 삼킴 → `!**/src/**/out/` 예외.
- **Gradle 9**: 테스트에 `junit-platform-launcher` 명시 필요.
- **Testcontainers ↔ 최신 Docker Desktop** 클라이언트 버전 불일치로 로컬 skip(CI는 정상).
