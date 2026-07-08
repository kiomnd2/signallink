# 구현 보고서 — domain-market 2년 백필 (슬라이스 2-③)

| 항목 | 내용 |
|------|------|
| 일자 | 2026-07-08 |
| 대상 | `domain-market` 2년 일봉 백필 커맨드 |
| 관련 | 플랜 `docs/M2-domain-market-plan.md` §9(7) · BACKLOG "KIS API 정책 준수 감사" |

## 1. 구현 내용
- **`PriceBackfillService`**: 상위 `limit`종목마다 [from,to]를 **100일 창으로 분할**해 `PriceCollectService.collectDaily` 반복 호출. 이미 데이터 있는 종목은 스킵(재개).
- 포트 확장: `StockRepositoryPort.findAll()`(유니버스), `DailyPriceRepositoryPort.existsByStockCode()`(재개 판단).
- **`BackfillRunner`**(app-collector): `signallink.backfill.enabled=true`(=`BACKFILL_ENABLED`)일 때만 등록되어 부팅 시 1회 실행. `limit`(기본 300)·`years`(기본 2) 설정.

## 2. 검증
- 빌드 SUCCESSFUL. 백필 단위 테스트 2건: 366일→100일 창 4개 분할 검증 / 이미 백필된 종목 스킵 → 통과.
- (포트 확장으로 기존 스텁 2곳에 신규 메서드 추가.)

## 3. 주요 결정·발견
- **100일 창**(정책③): KIS 기간별시세 1콜 최대 100건 → 100일(달력)이면 트레이딩일 <100로 truncation 없이 안전. 2년 ≈ 8창/종목.
- **재개**: `existsByStockCode`로 이미 적재된 종목 스킵 → 초당 1건 한도에서 중단 후 재실행 안전. 날짜 단위 멱등은 `collectDaily`가 담당.
- **수동 실행**: 매 부팅 자동 실행 방지 위해 `@ConditionalOnProperty`로 게이트. 운영에선 `BACKFILL_ENABLED=true`로 1회 실행 후 끔.

## 4. 잔여 (슬라이스 2 마무리 = 2-④ 예정)
- **vol_ratio_20d** 계산·wiring (`VolumeRatioCalculator` + 20영업일 이력)
- **일일 수집 스케줄러**(`@Scheduled`): J1(종목마스터)·일봉·지수 마감 후 수집, J7 베타(주간), 휴장일 캘린더
- **실호출 dry-run**: `.env` 키로 상위 소수 종목 백필 실행해 적재 확인
