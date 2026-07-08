# 구현 보고서 — domain-market vol_ratio_20d (M2 후속·M3 선결)

| 항목 | 내용 |
|------|------|
| 일자 | 2026-07-08 |
| 대상 | 거래량 비율(vol_ratio_20d) 계산·적재 |
| 관련 | 슬라이스 2 후속 · M3 3-A(특징주 "거래량 3배") 선결 |

## 1. 구현 내용
- **`VolumeRatioCalculator`**(순수): `vol_ratio = 당일 거래량 / 직전 최대 20영업일 평균`. 이력 없거나 평균 0이면 null.
- **`PriceCollectService.collectDailyWithVolRatio`**: 20영업일 prior 확보를 위해 from 이전 버퍼까지 KIS 조회 → **응답 시계열에서 인메모리로 평균 계산** → 신규 바에 vol_ratio 채워 저장. (포트/DB 조회 추가 없음 → 기존 스텁 무영향)
- **`DailyMarketCollectService`**: 일일 수집을 `collectDailyWithVolRatio`로 전환.

## 2. 검증
- 빌드 SUCCESSFUL. 테스트: 계산기 3(3배→3.00, 부분이력, null) + 수집 2(멱등 + vol_ratio 채움).

## 3. 결정·발견
- **포트 미변경 설계**: DB 읽기 포트를 늘리는 대신 수집 시 가져온 KIS 시계열로 계산 → 기능 인터페이스 스텁이 안 깨짐, 쿼리 부담도 없음.
- 백필 과거일 vol_ratio는 null 유지(특징주 선정은 당일 기준이라 무방, 과거 재현 필요 시 별도 패스).

## 4. 다음
M3 **3-A 특징주 선정** — 당일 daily_price에서 `±5%`(change_rate) 또는 `vol_ratio_20d ≥ 3` → 거래대금 상위 40. 이제 두 기준 모두 사용 가능.
