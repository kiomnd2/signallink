# M2 — domain-market 구현 플랜 (시세·수급·지수·베타)

| 항목 | 내용 |
|------|------|
| 작성일 | 2026-07-08 |
| 기준 | 구현플랜 M2 #1~3 · 기획확정서 v2.5 · 상세스펙 2장(스키마) · `docs/M0-kis-verification.md §4` · `docs/ARCHITECTURE.md` |
| 범위 | `domain-market`: 종목마스터(J1) · daily_price + **2년 백필** · index_price · investor_flow(잠정/확정) · stock_beta(J7) |
| 아키텍처 | 헥사고날 Level 1 — `domain-issue`와 동일 구조·패턴 재사용 |
| 핵심 | **KIS 실전 키 확보로 열림.** M3 Why 엔진(특징주·시장분해·수급진단·베타)의 **원천 데이터** = 크리티컬 패스 |

---

## 0. 왜 지금 / 무엇이 의존하는가

- M0에서 KIS TR·토큰·한도(초당 1건)가 확정됨 → market 착수 가능.
- M3 전부가 여기에 의존: 특징주 선정(거래대금 순위), 시장분해(등락률 − β×지수), 수급 진단(investor_flow), 도미노 리플레이·베타(2년 일봉).
- `common.ExternalApiClient` 상속 + `RateLimiter(1.0)` + 건별 예외 격리 등 **domain-issue에서 세운 패턴을 그대로 재사용**.

---

## 1. 선행 조건

- [x] KIS 실전 앱키 (M0 완료) — `.env`의 `KIS_APPKEY`/`KIS_APPSECRET`
- [ ] ⚠️ **미검증 TR 확인** — M0는 순위 2종·수급 2종만 실호출함. 아래는 **아직 실호출 안 됨** → 구현 전 `kis_verify.py`에 추가하거나 픽스처 확보 단계에서 실검증:
  - 일별시세(daily_price), 업종/지수 일별시세(index_price), **종목정보파일**(J1 마스터)
- 확정된 것(M0 §4): 종목별투자자 `FHKST01010900`(확정 수급), 외인·기관 가집계 `FHPTJ04400000`(장중 잠정), 거래량순위 `FHPST01710000`, 등락률순위 `FHPST01700000`

---

## 2. 대상 스키마 (기존 `V1__init.sql`, 변경 없음)

```
market.stock         (stock_code PK, name, market_type, sector_code, updated_at)
market.daily_price   (id, stock_code, trade_date, close, change_rate, volume, trading_value,
                      vol_ratio_20d, is_final, UNIQUE(stock_code, trade_date))   idx(trade_date)
market.index_price   (index_code, trade_date, close, change_rate, PK(index_code, trade_date))
market.investor_flow (id, stock_code, trade_date, investor_type, net_buy_qty, net_buy_amt,
                      is_final, UNIQUE(stock_code, trade_date, investor_type))    idx(trade_date)
market.stock_beta    (stock_code PK, beta_60d, calculated_at)
```
`UNIQUE` 제약이 백필·재수집의 **멱등** 기반. `is_final`로 장중 잠정→마감 확정 **덮어쓰기** 구분.

---

## 3. 구성 요소 (헥사고날 Level 1)

패키지 배치는 `docs/ARCHITECTURE.md §2`. `port/in` 생략(스케줄러가 유일 진입점), **`port/out` 필수**.

```
io.signallink.market
├─ domain/
│  ├─ Stock, DailyPrice, IndexPrice, InvestorFlow, StockBeta   (@Entity 겸 도메인)
│  ├─ InvestorType (enum: PERSON·FOREIGN·ORG)
│  └─ BetaCalculator, VolumeRatioCalculator                    (순수 도메인 계산 로직)
├─ application/
│  ├─ port/out/
│  │  ├─ Stock/DailyPrice/IndexPrice/InvestorFlow/StockBetaRepositoryPort
│  │  ├─ StockMasterGatewayPort      (종목정보파일)
│  │  └─ KisMarketGatewayPort        (일별시세·지수·투자자·가집계)
│  └─ service/
│     ├─ StockMasterSyncService      (J1)
│     ├─ DailyPriceCollectService    (일 수집 + 백필)
│     ├─ InvestorFlowCollectService  (잠정/확정 이원화)
│     └─ StockBetaService            (J7, 주간)
└─ adapter/out/
   ├─ persistence/   *JpaRepository + *PersistenceAdapter (5종)
   └─ external/kis/  KisTokenProvider + KisClient(들) + 응답 DTO(record)
```

### 빌드 의존성 (`domain-market/build.gradle.kts`)
`domain-issue`와 동일하게 추가: `spring-web`(RestClient), `spring-boot-starter-json`(파싱), 테스트: testcontainers-junit/postgresql, `testRuntimeOnly` postgresql. (루트 subprojects가 starter-test·junit-launcher 제공)

### 기존 예시 코드 재배치 (ARCHITECTURE §7)
현재 `io.signallink.market.Stock`·`StockRepository`(예시)를 → `Stock`은 `domain/`, `StockRepository`(Spring Data)는 `adapter/out/persistence/`로 이동 + `StockRepositoryPort` 신설. **이 재배치가 슬라이스 1의 첫 작업.**

---

## 4. KIS 연동 상세

### 4-1. 토큰 관리 (`KisTokenProvider`) — 최우선 인프라
- `POST /oauth2/tokenP` → access_token(24h). **재발급 제한 있음 → 반드시 캐시**(M0 §확인).
- 앱에서는 in-memory 캐시 + 만료 전 갱신, 재시작 대비 **DB 테이블 `kis_token`에 영속화**(§8 확정 — Flyway 마이그레이션 신설, app-api 소유). 모든 KIS 호출 헤더에 `authorization: Bearer`, `appkey`, `appsecret`, `tr_id`, `custtype:P` 부착.
- `KisClient`는 `ExternalApiClient` 상속 + `KisTokenProvider` 주입. `RateLimiter(1.0)`(안전 0.8, M0 실측 초당 1건).

### 4-2. TR 매핑
| 용도 | TR | 상태 | 비고 |
|------|-----|------|------|
| 종목마스터(J1) | 종목정보파일 | ⚠️ 미검증 | 다운로드+파싱(코드·명·시장·업종) → `stock` upsert |
| 일별시세 | 기간별시세(일) | ⚠️ 미검증 | 1콜당 ~100영업일 → 2년≈5콜/종목 |
| 지수 일별 | 업종/지수 기간별 | ⚠️ 미검증 | KOSPI·KOSDAQ 등 소수 index_code |
| 확정 수급 | `FHKST01010900` | ✅ M0 | 당일치는 **장마감 후** 제공 → `is_final=true` |
| 장중 잠정 수급 | `FHPTJ04400000` | ✅ M0 | 시장 단위(순매수 상위), 하루 4~5회 갱신 → `is_final=false` |

### 4-3. 2년 백필 전략 (rate limit 영향 큼)
- 초당 1건 → 대상 **거래대금 상위 ~300종목**(§8 확정)×5콜 ≈ 1,500콜 ≈ **25분**. **수동 실행 커맨드**로, 멱등(`UNIQUE`)·**재개 가능**(마지막 성공 종목 이후부터)·진행 로그.
- 지수(index_price) 백필은 소수 index_code라 저렴. 유니버스 부족 판명 시 대상만 확대 후 재실행(멱등).

---

## 5. 수급 이원화 (스펙 4단계 반영)

- **장중**: 가집계 TR로 잠정 저장(`is_final=false`). 시장 단위 조회라 순매수 상위 종목만 채워질 수 있음 + 하루 4~5회만 갱신 → "수급 갱신 시각" 개념 유지.
- **장마감 후**: `inquire-investor`로 종목별 확정치 저장/덮어쓰기(`is_final=true`).
- `investor_type` = 개인/외국인/기관 (응답의 `prsn_/frgn_/orgn_ntby_qty`·`_tr_pbmn` → net_buy_qty/amt).

---

## 6. 도메인 계산 (순수 로직, 단독 테스트)

- **`VolumeRatioCalculator`**: `vol_ratio_20d` = 당일 거래량 / 최근 20영업일 평균. daily_price 적재 후 계산.
- **`BetaCalculator`**: `beta_60d` = Cov(종목 일수익률, 지수 일수익률)/Var(지수) (최근 60영업일). 2년 백필 데이터로 **과거 날짜 재현 검증** 가능(J7).

---

## 7. 테스트 전략

- [ ] **파서 단위 테스트**: KIS 실응답 JSON을 `src/test/resources/fixtures/`에 저장(M0 실호출로 확보) → DTO·매핑 검증
- [ ] **계산 단위 테스트**: `BetaCalculator`(알려진 수익률 → 기대 β), `VolumeRatioCalculator`
- [ ] **영속 테스트**: `@DataJpaTest`(Testcontainers) — `UNIQUE`·is_final 덮어쓰기 확인
- [ ] **드라이런 스크립트**: 하루 수집 후 row 수·null 비율·잠정→확정 전환 확인

---

## 8. 결정 사항 (확정, 2026-07-08)

1. **백필 유니버스** → ✅ **거래대금 상위 ~300종목**으로 시작(≈25분). 부족 시 확대 (특징주는 대부분 상위에서 발생). 상위 목록은 거래량순위 TR(`FHPST01710000`, 거래대금순)로 산출.
2. **KIS 토큰 영속화** → ✅ **DB 테이블 `kis_token`** (Flyway 마이그레이션 1개 신설, app-api 소유). 재시작·컨테이너에 강하고 볼륨 불필요.

---

## 9. 구현 순서 (슬라이스 · 체크리스트)

**슬라이스 1 — 토큰 + 종목마스터(J1)** *(인증·stock 기반)*
1. [ ] build.gradle 의존성 + 기존 `Stock`/`StockRepository` 헥사고날 재배치 + `StockRepositoryPort`
2. [ ] `KisTokenProvider`(발급 + **DB `kis_token` 캐시**, Flyway 마이그레이션 신설) + 실호출 검증
3. [ ] `StockMasterGatewayPort` + 종목정보파일 어댑터 + `StockMasterSyncService`(J1)

**슬라이스 2 — 일별시세 + 지수 + 백필** *(KIS 정책 준수 ①유량·③건수 — BACKLOG "KIS API 정책 준수 감사" 참조)*
4. [ ] `DailyPrice`/`IndexPrice` 엔티티·포트·영속 어댑터 + `@DataJpaTest`
5. [ ] 일별시세·지수 TR 어댑터 — **`ExternalApiClient` 상속 + `RateLimiter(1.0)`**(정책①) + 파서 픽스처 테스트 + **TR 실검증 포함**
6. [ ] `DailyPriceCollectService`(일 수집) + `VolumeRatioCalculator`
7. [ ] **2년 백필 커맨드** — **기간을 100건(≈영업일) 단위로 분할 요청**(정책③ 조회 건수 제한) + 멱등·재개·진행로그

**슬라이스 3 — 수급**
8. [ ] `InvestorFlow` 엔티티·포트 + `InvestorFlowCollectService`(가집계 잠정 / inquire-investor 확정, is_final 이원화)

**슬라이스 4 — 베타(J7)**
9. [ ] `BetaCalculator` + `StockBetaService`(주간) — 백필 데이터로 재현 검증

**스케줄러(collector)**
10. [ ] `app-collector`에 J1(일 1회)·일별시세 수집(마감 후)·수급(장중 30분+마감)·J7(주간) `@Scheduled` + 휴장일 캘린더

각 슬라이스는 `domain-issue`처럼 **PR 단위**로 끊고, 완료 후 이 문서 §9와 `docs/1.이후-구현플랜.md`를 갱신(문서 동기화 규칙).

---

## 10. Exit 조건

- 평일 하루 무인 방치 → `stock`·`daily_price`·`index_price`·`investor_flow`에 당일 데이터 정상 적재, **잠정→확정 덮어쓰기** 확인
- 2년 백필 완료 + `stock_beta`가 백필 데이터로 계산·검증됨
- 파서·계산·영속 단위 테스트 그린 + CI 통과

---

## 11. 리스크 → 대응

| 리스크 | 대응 |
|--------|------|
| 초당 1건 한도로 백필 장시간 | 유니버스 축소(상위 N) 시작 + 야간 1회 + 재개 가능 |
| 미검증 TR(일별시세·지수·마스터) 스펙 상이 | 슬라이스 착수 시 실호출로 먼저 확인(M0 방식), 픽스처 저장 |
| 장중 가집계 시장 단위·저빈도 | 잠정은 상위 종목 한정 + "갱신 시각" 표기, 확정으로 마감 후 보정 |
| KIS 토큰 재발급 제한 | 영속 캐시 + 만료 전 갱신 |
