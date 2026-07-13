# M3 3-C 수급 진단 구현 보고

| 항목 | 내용 |
|------|------|
| 작성일 | 2026-07-13 |
| 범위 | Why 엔진 3-C — 투자자 수급을 초보자용 `flow_summary` 문장으로 진단 |
| 결과 | ✅ 구현·테스트 완료 (단위 10건 그린), PR 대기 |

## 구현 내용

`insight → market` 읽기는 3-B와 동일하게 **market이 소유한 조회 포트**를 통한다.

### domain-market (수급 소유 도메인)
- `InvestorFlowQueryPort`(신규): `flowsOn(stockCode, tradeDate)` → 투자자별 수급 행
- `InvestorFlowRow`(신규 읽기 DTO): `investorType·netBuyQty·netBuyAmt·isFinal`
- `InvestorFlowPersistenceAdapter`가 포트 구현 추가(`findByStockCodeAndTradeDate` JPA 파생 쿼리)

### domain-insight (Why 엔진)
- `FlowDiagnoser`(순수 도메인): 외국인·기관 순매수 금액(백만원) → 초보자 문장
  - 개인(PERSON) 제외 — 통상 외국인·기관의 반대편(노이즈) + 장중 가집계는 외국인·기관만 제공
  - `|순매수| < 1억(100백만원)` → 중립 처리
  - 동반 순매수/순매도(우호·부담) vs 엇갈림 뉘앙스 문장
  - `provisional=true`(장중 가집계)면 "장중 잠정 집계로는, " 접두
  - **조언(매수/매도/목표가) 문구 없음** — 테스트로 금지어 검증
- `FlowDiagnosis`(결과): `flowSummary` + `provisional`(카드 "잠정" 뱃지용)
- `FlowDiagnoseService`(유스케이스): 수급 행 조회 → 외국인·기관 집계 + 확정치 없이 가집계만이면 잠정 판정 → `FlowDiagnoser` 위임

## 주요 결정
- **외국인·기관만 본다**: 개인 수급은 초보자 카드에 노이즈이고, 장중 잠정(가집계 TR)이 외국인·기관만 주므로 장중·마감 경로가 일관됨.
- **잠정 판정 근거**: 조회 행에 `is_final=false`가 하나라도 있으면 잠정. 마감 확정(upsert)이 덮어쓰면 자동으로 확정 문장으로 전환.
- **금액 단위**: `net_buy_amt`는 KIS 응답 그대로 백만원 → 억 원으로 환산(반올림)해 표기.

## 검증
- `FlowDiagnoserTest`(7건): 동반 매수/매도·엇갈림·중립·잠정·무데이터·금지어 부재
- `FlowDiagnoseServiceTest`(3건): 확정 집계·가집계 잠정 판정·무데이터
- `./gradlew :domain-market:test :domain-insight:test` + 전체 컴파일 그린

## 잔여
- 3-D 이슈 매칭(공시·뉴스 시간창) → `source_refs`
- 3-F J2 파이프라인에서 `FlowDiagnosis`를 `feature_card.flow_summary`로 upsert(잠정 뱃지 반영)
