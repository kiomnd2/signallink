# 구현 보고서 — domain-market 일별시세·지수 (슬라이스 2 ①②)

| 항목 | 내용 |
|------|------|
| 일자 | 2026-07-08 |
| 대상 | `domain-market` 슬라이스 2 — 일봉(daily_price)·지수(index_price) 영속 + 수집 |
| 관련 | PR #10(2-①), PR(2-②) · 플랜 `docs/M2-domain-market-plan.md` §9 |
| 아키텍처 | 헥사고날 Level 1 (`docs/ARCHITECTURE.md`) |

## 1. 구현 내용

**2-① 영속 계층** (PR #10, 머지됨)
- `DailyPrice`(market.daily_price) · `IndexPrice`(market.index_price, `(index_code,trade_date)` 복합키 `@IdClass`)
- `DailyPriceRepositoryPort`·`IndexPriceRepositoryPort` + JPA 영속 어댑터

**2-② 수집** (이 PR)
- `KisMarketClient` — 일별시세(`FHKST03010100`)·지수(`FHKUP03500100`) TR 호출. `ExternalApiClient` + `RateLimiter(1.0)` + `KisTokenProvider` Bearer/헤더
- `KisDailyPriceResponse`·`KisIndexPriceResponse` — output2 매핑 + **등락률 계산**
- `DailyPriceGatewayPort`·`IndexPriceGatewayPort` + `PriceCollectService`(기존 날짜 스킵 = 멱등)

## 2. 검증
- 빌드 SUCCESSFUL. 단위 테스트: 일봉/지수 파서(등락률) 2, 수집 서비스 1, (기존 토큰·마스터·영속 유지) → **전부 통과**
- Testcontainers 5건(영속)은 로컬 Docker 환경 이슈로 skip → CI 실행
- **KIS 실검증 완료**: 두 TR `rt_cd=0`, output2 필드 실측 확인(삼성전자 일봉·KOSPI 지수)

## 3. 주요 결정·발견
- ⚠️ **등락률은 응답에 없음** → 일봉은 전일대비(`prdy_vrss`)로, 지수는 직전 행 종가 대비로 **계산**. (실검증으로 확인 — 추측했으면 틀렸을 부분)
- 정책 준수: **① 유량** = `RateLimiter(1.0)`(M0 실측). **③ 조회 100건** = 백필에서 100영업일 단위 페이지네이션(2-③)
- 🐛 **`.env` 따옴표 사고**: 검증 중 `.env` 값이 `"..."`로 감싸여 있어 수동 파싱 시 EGW00103(유효하지 않은 AppKey) 발생 → 셸 `source`는 따옴표를 벗기지만 일부 로더는 아님 → `.env` 따옴표 제거로 해결. (KIS 발급 제한/차단 아니었음)

## 4. 잔여 (2-③ 및 후속)
- **2년 백필 커맨드**: 100영업일 단위 분할·상위 ~300종목·멱등·재개 (BACKLOG 정책③)
- **vol_ratio_20d**: 20영업일 이력 필요 → 백필 후 계산 (`VolumeRatioCalculator`)
- collector 스케줄러(J1/일봉/J7) `@Scheduled` 배선 + 실호출 dry-run
