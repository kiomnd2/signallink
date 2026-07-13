# 구현 보고서 — M3 3-B 시장분해·업종판별

| 항목 | 내용 |
|------|------|
| 일자 | 2026-07-08 |
| 대상 | 특징주가 "왜" 움직였나 — 시장 탓 vs 개별 이슈 분해 + 업종 동반등락 |
| 산출 | feature_card의 `market_contrib`·`sector_sync`·`what_happened` 재료 |

## 1. 핵심 로직
- **시장분해**(`MarketDecomposer`, 순수): `market_contrib = β × 지수등락률`(시장 흐름으로 설명되는 몫), `excess = 등락률 − market_contrib`(개별 요인). 두 절대값 비교로 주된 동인 판정 → `Driver`(MARKET_DRIVEN / STOCK_SPECIFIC). β·지수 없으면 전부 개별.
- **업종 동반**(`SectorSyncEvaluator`, 순수): 같은 업종(자기 제외) 등락률로 (1)표본 ≥3, (2)같은 방향 비율 ≥0.6, (3)평균 절대값 ≥1% → sector_sync.
- **문장**(`WhatHappenedComposer`, 순수): 분해 결과를 초보자 언어로. 조언 금지어(매수/매도/목표가) 미포함(테스트로 강제).

## 2. 조회 배선 (market 신규 포트 4개 — 전부 additive, 기존 스텁 무영향)
- `StockInfoQueryPort`(시장·업종), `StockBetaQueryPort`(β), `IndexChangeQueryPort`(지수 당일 등락률), `SectorMoveQueryPort`(업종 동반 — `daily_price ⋈ stock` 조인, 자기 제외).
- 지수 코드 매핑은 `MarketIndex` enum(market.domain): KOSPI→0001·KOSDAQ→1001, 라벨 코스피/코스닥.
- **유스케이스** `MarketDecomposeService.analyze(FeatureCandidate) → MarketAnalysis`: 포트 4개 주입 → 분해·동반·문장 조립.

## 3. 검증 (전체 빌드 SUCCESSFUL, insight 23)
- 분해 4(시장주도/개별주도/β증폭/데이터없음), 업종 4(동반/방향혼재/미미/표본부족), 문장 5(시장·개별·폴백·업종·금지어), 서비스 2(전체 흐름/폴백).

## 4. 결정·발견
- **동인 판정 = 절대값 비교**: `|market_contrib| ≥ |excess|`면 시장주도. 초보자에게 "시장 때문 vs 이 종목 때문"을 한 눈에.
- **지수 코드는 market 소유 enum으로**: collector yaml의 매직값(0001/1001)과 중복되던 것을 도메인 상수로. (컨벤션: 매직값→상수 클래스)
- **업종 조인은 market 내부 쿼리**: insight는 `SectorMoveQueryPort` 결과(등락률 리스트)만 받고, 조인은 소유 도메인(market)이 수행 → 경계 유지.
- 아직 feature_card 영속은 없음 — 분석 결과 record만 산출(카드 upsert는 3-F).

## 5. 다음
**3-C 수급 진단** — `investor_flow`(외국인·기관·개인) 순매수 주체·규모 → `flow_summary`(장중은 "잠정"). market/issue 조회 포트로 investor_flow를 읽어 문장화.
