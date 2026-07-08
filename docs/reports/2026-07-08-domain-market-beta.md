# 구현 보고서 — domain-market 베타 (슬라이스 4)

| 항목 | 내용 |
|------|------|
| 일자 | 2026-07-08 |
| 대상 | stock_beta (J7 주간 베타 계산) |
| 관련 | 플랜 `docs/M2-domain-market-plan.md` §6·§9(9) |

## 1. 구현 내용
- **`BetaCalculator`**(순수 도메인): β = Cov(종목 일수익률, 지수 일수익률)/Var(지수). 종목·지수 종가를 공통 거래일로 정렬→일수익률→계산. 수익률 20개 미만·분산 0이면 null.
- **`StockBeta`**(@Entity, stock_code PK → save가 merge=upsert).
- **조회 포트**: `DailyPriceQueryPort`·`IndexPriceQueryPort`(recentCloses) — 기존 영속 어댑터가 추가 구현(기존 포트/스텁 무변경).
- **`StockBetaService`**(J7): 유니버스 상위 limit종목마다 시장 지수(KOSPI 0001/KOSDAQ 1001)로 60일 베타 계산·저장. 종목 단위 예외 격리.
- **`StockBetaJob`**(collector `@Scheduled` 토 06:00 KST) + 설정 `signallink.beta.*`.

## 2. 검증
- 빌드 SUCCESSFUL. 테스트: 베타 계산(종목=지수 2배 → **β=2.000**, 데이터 부족 → null) 2 · 서비스 1 · upsert Testcontainers 1(CI).

## 3. 주요 결정·발견
- 등락률과 마찬가지로 **베타는 자체 계산**(수익률 → Cov/Var). 백필 2년 데이터로 과거 재현 검증 가능.
- 조회 기능을 **별도 QueryPort**로 분리 → 기존 RepositoryPort/스텁을 건드리지 않고 확장(포트 분리의 이점).
- `stock_beta`는 PK가 stock_code라 `save`가 곧 upsert(merge) — investor_flow처럼 별도 find-update 불필요.

## 4. domain-market 상태 & 잔여
- ✅ **원천 데이터 전부 구현**: 종목마스터(J1)·일봉·지수·2년백필·무인수집·수급(잠정/확정)·베타(J7).
- 잔여(후속): **vol_ratio_20d** 계산·wiring · 수급 수집 스케줄러 배선(잠정 30분/확정 마감후) · 실호출 dry-run · `KisClient` 공통 베이스 추출(BACKLOG).
- 다음 마일스톤: **M3 Why 엔진**(특징주 선정·시장분해·수급진단·이슈매칭·LLM) — 이제 원천 데이터가 준비됨.
