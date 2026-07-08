# 구현 보고서 — M3 3-A 특징주 선정

| 항목 | 내용 |
|------|------|
| 일자 | 2026-07-08 |
| 대상 | domain-insight 첫 유스케이스 — 당일 특징주 선정 |
| 규칙 | 등락률 ±5% **또는** vol_ratio_20d ≥ 3 → **거래대금 상위 40** |

## 1. 구현 내용
- **market `StockDaySnapshot` + `DailyPriceSnapshotQueryPort`**(신규 아웃바운드 포트): 특정 거래일 전 종목 일봉 요약(종목·등락률·vol_ratio·거래대금) 조회. `DailyPricePersistenceAdapter`가 `findByTradeDate`로 구현.
- **insight `TriggerType`**(SURGE/VOLUME) + **`FeatureCandidate`**(선정 결과 record).
- **insight `FeatureStockSelector`**(순수): 트리거 판정(±5% → SURGE, 3배 → VOLUME, 둘 다면 SURGE 우선) → 거래대금 내림차순 상위 N.
- **insight `FeatureStockSelectService`**(@Service): market 조회 포트 주입 → 스냅샷 읽어 선정. 이 결과가 3-B~3-F 입력.

## 2. 검증 (빌드 SUCCESSFUL)
- `FeatureStockSelectorTest` 7: SURGE(+/−), VOLUME, 둘다→SURGE, 미달 제외, vol_ratio null 무시, 상위 N 순서.
- `FeatureStockSelectServiceTest` 1: 스텁 포트 → 트리거 종목만 거래대금순 선정.

## 3. 결정·발견
- **도메인 간 읽기 = 소유 도메인 QueryPort 주입**(플랜 §0 확정 방식). insight는 market DB 직접 접근 없이 `DailyPriceSnapshotQueryPort`만 의존 → 경계 유지.
- **신규 전용 포트로 분리**: 기존 `DailyPriceQueryPort`(람다 스텁 사용처 있음)에 메서드를 더하지 않고 별도 포트를 만들어 기존 스텁 무영향.
- **거래대금 정렬을 선정 기준으로**: "±5%/3배"는 관문, 최종 노출은 거래대금 큰 순 → 초보자가 주목할 종목이 자연히 상위.
- 아직 feature_card 영속은 없음 — 선정 후보만 산출(카드 생성은 분석 완료 후 3-F 파이프라인).

## 4. 다음
**3-B 시장분해·업종판별** — `market_contrib = β × 지수등락률`, 초과등락 해석 + 업종 동반등락(sector_sync) → `what_happened` 템플릿. (β는 stock_beta, 지수등락률은 index_price 조회 포트 필요)
