# M3 — domain-insight 구현 플랜 (Why 엔진)

| 항목 | 내용 |
|------|------|
| 작성일 | 2026-07-08 |
| 기준 | 구현플랜 M3 · 기획확정서 v2.5 · 상세스펙 3장 · `docs/ARCHITECTURE.md` |
| 범위 | 특징주 선정 → 시장분해·업종판별 → 수급진단 → 이슈매칭 → LLM 요약 → J2 파이프라인 (feature_card 생성) |
| 성격 | **서비스의 심장 · 최다 리스크 구간.** 백필 데이터로 과거 재현 검증하며 진행 |

## 0. 도메인 간 읽기 방식 (해결)
`domain-insight`는 Gradle로 market·issue·macro를 의존(읽기). **각 도메인이 노출한 조회 포트(QueryPort)를 insight 서비스가 주입받아 읽는다** — insight는 타 도메인 DB에 직접 접근하지 않는다. 필요한 조회는 소유 도메인에 QueryPort로 추가:
- market: `todayMovers()`(당일 등락/거래대금), 기존 `DailyPrice/IndexPriceQueryPort`, stock_beta·stock(업종) 조회
- issue: 종목·시간창 기준 공시/뉴스 조회 포트

## 1. 대상 스키마 (기존 V1)
`insight.feature_card`(stock_code, trade_date, change_rate, trigger_type, market_contrib, sector_sync, what_happened, flow_summary, context_note, source_refs jsonb, llm_used, status, UNIQUE(stock_code,trade_date)) · `insight.health_check`

## 2. 서브슬라이스 (구현 순서)
- **3-A 특징주 선정**: 당일 `daily_price`에서 **±5% 또는 거래량 3배** → **거래대금 상위 40** 선정. (거래량 3배 = `vol_ratio_20d` 필요 → §4 선결)
- **3-B 시장분해·업종판별**: `market_contrib = β × 지수등락률`, 초과분(등락률−market_contrib) 해석 + 업종 동반등락(sector_sync) → `what_happened` 템플릿 문장
- **3-C 수급 진단**: ✅ `investor_flow`(외국인·기관) → `flow_summary`(장중은 "잠정"). `FlowDiagnoser`(순수) + `FlowDiagnoseService`, market `InvestorFlowQueryPort`로 읽기. 개인은 노이즈·가집계 미제공이라 제외, |순매수|<1억은 중립.
- **3-D 이슈 매칭**: ✅ 공시 우선 + 뉴스 시간창 필터 → 후보 정리 → `source_refs`(jsonb). `IssueMatcher`(순수, 공시 우선·최신순·최대 3건) + `IssueMatchService`(거래일 KST 하루 시간창), issue `IssueQueryPort`로 읽기. jsonb 직렬화는 3-F에서.
- **3-E LLM 클라이언트**: ✅ 페이로드 조립 → Gemini `generateContent` → JSON 파싱 → **금지어(매수/매도/목표가) 필터** → 실패 시 템플릿 폴백 → **일 상한 가드**. `WhyContext`+`WhyPromptComposer`+`BannedWordFilter`(순수) + `WhySummaryService`(뉴스/공시 있는 종목만·거래일 상한) + `GeminiClient`(`LlmGatewayPort`). 기본 `LLM_ENABLED=false`(키 발급 전 템플릿 유지). **실호출·모델 ID 검증은 키 발급 후 잔여**(BACKLOG). 보고서 `docs/reports/2026-07-22-m3-3e-llm-summary.md`
- **3-F J2 파이프라인**: ✅ 30분 배치, 선정→분석→카드 upsert, **종목 단위 예외 격리** + Discord 실패 알림. `FeatureCardPipelineService`(3-A~3-D 조립) + `FeatureCardJob`(app-collector, 장중 30분). LLM(3-E) 전이라 what_happened는 템플릿 폴백·`llm_used=false`. source_refs는 jsonb(수동 직렬화, 어댑터).
- (3-G health_check · 3-H macro는 후속 — 플랜상 5번(LLM)까지면 M4 진행 가능)

## 3. 계산·판정 로직 (순수 도메인, 단독 테스트)
- **시장분해**: `초과등락 = change_rate − beta_60d × index_change_rate`. market_contrib 저장.
- **트리거 판정**: `trigger_type` ∈ {SURGE(±5%), VOLUME(거래량 3배)}.
- **수급 진단**: 순매수 주체/규모 → 문장 매핑.
- 전부 `BetaCalculator`처럼 순수 계산 클래스로.

## 4. 선결/결정 필요
1. **vol_ratio_20d 계산** — 3-A의 "거래량 3배" 기준에 필요. M2 후속으로 미뤄둔 것 → **3-A 전에 wiring**(일봉 적재 시 20일 평균 대비 계산) 또는 3-A는 우선 ±5%+거래대금만으로 시작.
2. **Gemini 키** — 3-E LLM에 필요(즉시 발급 가능). 없으면 3-A~3-D + 템플릿 폴백까지 먼저.
3. **LLM 모델·일 상한** — Gemini 3 Flash, 일 20~30건(뉴스 있는 종목만), `signallink.llm.daily-limit`(이미 설정 슬롯 존재).

## 5. 테스트 전략
- 순수 판정/계산(시장분해·트리거·수급문장) 단위 테스트 (백필 픽스처로 **과거 유명 급등락 재현**).
- 선정 쿼리: 백필 데이터로 "그날 특징주가 맞게 뽑히나" 검증.
- LLM: 프롬프트→출력 금지어 필터 + 폴백 경로 테스트(스텁 LLM).

## 6. Exit (M3)
장중 30분마다 카드 40개 자동 생성·갱신, LLM 출력 20건 검수 시 조언성 0건, 과거 급등락 3건 재현. (기한 말 기준 3-E까지면 M4 진행, health_check·macro 병행)

## 7. 착수 제안
**3-A(특징주 선정) 전에 vol_ratio_20d를 먼저 완성**(M2 후속)하는 것을 권장 — 3-A의 트리거 기준이 완전해짐. 이후 3-A→3-B→3-C→3-D→(키 준비 시)3-E→3-F 순.
