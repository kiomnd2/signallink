# 구현 보고서 — domain-market 수급 investor_flow (슬라이스 3)

| 항목 | 내용 |
|------|------|
| 일자 | 2026-07-08 |
| 대상 | 투자자 수급(개인·외국인·기관) 이원화 수집 |
| 관련 | 플랜 `docs/M2-domain-market-plan.md` §5 · M0 검증 TR 2종 |

## 1. 구현 내용
- **도메인**: `InvestorType`(PERSON/FOREIGN/ORG), `InvestorFlow`(@Getter + `update()` — 덮어쓰기용). `(stock,date,type)` 유니크.
- **확정**(`inquire-investor` FHKST01010900): 종목별 최신 거래일의 3종 순매수(수량·금액) → `is_final=true`.
- **잠정**(가집계 FHPTJ04400000): 시장 순매수 상위 리스트의 **외국인·기관**(개인 없음) → `is_final=false`.
- **`InvestorFlowCollectService`**: `collectConfirmed(stockCode)` / `collectProvisional()`. 건별 예외 격리.
- **`InvestorFlowPersistenceAdapter.upsert`**: (stock,date,type) 있으면 값 갱신, 없으면 삽입 → **잠정→확정 덮어쓰기**.

## 2. 검증
- 빌드 SUCCESSFUL. 테스트: 확정 파서 1 · 잠정 파서 1 · 수집 서비스 2(is_final 구분) · **upsert 덮어쓰기 Testcontainers 1**(잠정→확정 시 단일 행 갱신, is_final flip).
- **KIS 실검증**(15:38 KST): 두 TR `rt_cd=0`, 실필드 확인 — 확정(prsn/frgn/orgn_ntby_qty·_tr_pbmn), 잠정(mksc_shrn_iscd·frgn/orgn_ntby_qty·_tr_pbmn).

## 3. 주요 결정·발견
- **잠정은 외국인·기관만**(개인 미제공) + 시장 단위 상위 리스트 → 상위 종목만 잠정값 존재. 확정은 종목별 3종.
- **덮어쓰기**: 유니크 키 기준 upsert로 장중 잠정을 마감 확정이 대체(플랜 §5 Exit "잠정→확정 덮어쓰기").
- `net_buy_amt` 단위는 KIS 그대로 **백만원**.
- `KisInvestorClient`가 `KisMarketClient`와 KIS 헤더/토큰 구성이 중복 → **공통 `KisClient` 베이스 추출**을 BACKLOG에 남길 것.

## 4. 잔여
- **수급 수집 스케줄러 배선**: 잠정=장중 30분 주기, 확정=마감 후(universe 순회). (collector `@Scheduled`)
- vol_ratio_20d 계산 · 슬라이스 4(stock_beta J7) · 실호출 dry-run
