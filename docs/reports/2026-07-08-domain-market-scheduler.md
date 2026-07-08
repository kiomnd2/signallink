# 구현 보고서 — domain-market 일일 수집 스케줄러 (슬라이스 2-④)

| 항목 | 내용 |
|------|------|
| 일자 | 2026-07-08 |
| 대상 | 일일 시세 무인 수집 배치 (슬라이스 2 마무리) |
| 관련 | 플랜 `docs/M2-domain-market-plan.md` §9(10) |

## 1. 구현 내용
- **`DailyMarketCollectService`**(domain-market): 유니버스 상위 `limit`종목의 최근 `recentDays`일 일봉 + 지정 지수 수집. 종목/지수 단위 예외 격리. 최근 며칠 겹쳐 조회 → 누락일 보정(멱등).
- **`MarketCollectJob`**(app-collector): `@Scheduled(cron=평일 16:30 KST)` → J1 종목마스터 sync + 일일 시세 수집. cron의 `MON-FRI`가 주말 가드, 공휴일은 멱등 수집이 흡수.
- `HeartbeatJob` 제거(실제 잡으로 대체). 설정: `signallink.collect.*`(cron·limit·recent-days·index-codes=0001 KOSPI/1001 KOSDAQ).
- 테스트: 일일 수집 합산(종목 2 + 지수 1 = 3건).

## 2. 검증
- 빌드 SUCCESSFUL, 단위 테스트 통과. `@EnableScheduling`은 `SignalCollectorApplication`에 기존 적용됨.

## 3. 주요 결정·발견
- **휴장일**: cron 요일 지정(MON-FRI)으로 주말 제외. 공휴일 전용 캘린더는 미도입 — 멱등 수집이라 공휴일 실행해도 신규 0건(무해). 정밀 캘린더는 후속.
- **누락 보정**: 최근 5일 겹쳐 조회 → 배치 1~2회 건너뛰어도 다음 실행에서 메움.

## 4. 슬라이스 2 상태 & 잔여
- ✅ 슬라이스 2(일별시세·지수·백필·무인 수집) **Exit 달성**: 평일 자동으로 daily_price·index_price 적재.
- 잔여(별도 슬라이스/후속):
  - **vol_ratio_20d** 계산·wiring (`VolumeRatioCalculator` + 20영업일 이력) — M3 특징주 선정에 사용
  - **슬라이스 3**: investor_flow(수급, 잠정/확정)
  - **슬라이스 4**: stock_beta(J7 주간)
  - **실호출 dry-run**: `BACKFILL_ENABLED=true` 또는 배치 1회로 실적재 확인(로컬 DB 필요)
