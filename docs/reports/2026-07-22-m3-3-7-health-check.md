# 구현 보고서 — M3 3-7 체력 진단 (이슈 성격 태그 · 실적 추세) (2026-07-22)

| 항목 | 내용 |
|------|------|
| 대상 | `domain-insight` 체력 진단 + `domain-issue` DART 재무 소스 |
| 상태 | ✅ 3-7a(이슈 성격 태그)·3-7c(실적 추세) 배선·테스트 완료. 실호출·corp_code 매핑은 후속 |
| 브랜치 / PR | `feat/insight-health-check` / (PR) |
| 관련 | 플랜 `docs/M3-health-check-macro-plan.md`, 스펙 기획확정서 §체력 진단 경량판 |

---

## 1. 배경

Why 카드에 "이 악재 오래 갈까?" 판단 재료(체력 진단 3종 팩트)를 붙이는 작업. 이번 슬라이스는 3종 중 두 개:
**① 이슈 성격 태그**(3-7a), **② 실적 추세**(3-7c). ③ 수급 추세(flow_after, J6)는 후속.

health_check는 카드(feature_card)에 `card_id`로 1:1로 딸린다. 그래서 파이프라인이 카드 upsert 후 그 id로
진단을 기록해야 한다 → `FeatureCardRepositoryPort.upsert`가 **card_id를 반환**하도록 바꾼 게 선결이었다.

## 2. 3-7a 이슈 성격 태그

- `IssueNature`(enum): `ONE_OFF`(일회성)·`STRUCTURAL`(구조적)·`UNCLASSIFIED`(분류불가).
- `IssueNatureClassifier`(순수): 매칭된 공시·뉴스(`IssueRef`)의 제목·카테고리를 룰로 훑는다.
  **구조적 키워드(적자·감자·상장폐지·규제·횡령…)가 하나라도 있으면 STRUCTURAL**(오래 갈 이슈를 놓치지 않는
  보수적 우선), 일회성 키워드(공급계약·수주·자사주…)만 있으면 ONE_OFF, 근거 없거나 애매하면 UNCLASSIFIED.
- 영속: `HealthCheck`(엔티티) + `HealthCheckRepositoryPort`/`HealthCheckUpsert` + `HealthCheckPersistenceAdapter`
  (card_id 기준 upsert 멱등).
- 배선: J2 파이프라인이 카드 upsert 직후 태그를 기록.
- (LLM 보조 분류는 후속 — 현재 룰 기반. 애매 케이스를 LLM으로 재분류하는 것은 BACKLOG.)

## 3. 3-7c 실적 추세 (재무제표 표현 + 분석)

**"재무제표를 가져와 표현"** — issue 도메인(DART):
- `QuarterAccount`(분기 매출·영업이익)·`CompanyFinancials`(최신순 시계열) — 표현 모델.
- `DartFinancialGatewayPort` + `DartFinancialClient`(`ExternalApiClient` 상속) — DART 단일회사 주요계정
  (`fnlttSinglAcnt.json`) 호출 → 매출액·영업이익 추출. **연결(CFS) 우선**, 콤마 금액 파싱, 013=empty,
  그 외 오류=예외(공시 어댑터와 동일 원칙).
- `FinancialQueryPort` + `DartFinancialQueryService` — corp_code 해석 후 최근 2개 사업연도의 분기/사업
  보고서를 최신순으로 훑어 최대 4분기 수집.
- `CorpCodeResolver` + `NoOpCorpCodeResolver` — stock_code→corp_code. **실 매핑(corpCode.xml)은 BACKLOG**,
  그 전까지 empty → 재무 조회 생략.

**"재무제표를 분석"** — insight 도메인:
- `FinancialTrend`(개선/유지/악화/데이터부족) + `FinancialTrendAnalyzer`(순수): 최신값 vs 가장 오래된값의
  상대 변화가 ±5% 밖이면 개선/악화. **적자↔흑자 부호 변화도 처리**(적자 축소=개선). 데이터 2개 미만은 UNKNOWN.
- `FinancialTrendService`: `signallink.health.financials-enabled`(기본 false) 게이트 + 조회 실패 시 empty(폴백).
- 배선: 파이프라인이 실적 추세를 계산해 health_check `revenue_trend`/`profit_trend`에 기록(비활성/미매핑 시 null).

## 4. 설계 결정

- **card_id 반환**: `upsert`가 void → long. health_check FK를 파이프라인에서 바로 연결.
- **기본 off (`financials-enabled=false`)**: corp_code 리졸버·DART 재무 키 준비 전까지 재무 호출 생략 →
  "데이터 없음"으로 우아하게 비표시. LLM(3-E)과 동일한 운영 스위치 패턴.
- **보수적 태그**: 구조적 신호 우선 — 오래 갈 악재를 일회성으로 오분류하지 않는 쪽.
- **팩트만**: 결론(보유/매도) 생성 없음 — 태그·추세 값만 저장.

## 5. 검증 결과

`./gradlew build` — BUILD SUCCESSFUL. 신규/갱신 테스트 통과(회귀 없음).

| 테스트 | 건수 | 커버 |
|--------|------|------|
| `IssueNatureClassifierTest` | 5 | 일회성·구조적·분류불가·보수 우선·키워드 없음 |
| `HealthCheckPersistenceTest` | 2 | card_id FK·upsert 갱신 (Testcontainers, 로컬 skip·CI 실행) |
| `FinancialTrendAnalyzerTest` | 5 | 개선·악화·유지·적자축소·데이터부족 |
| `DartFinancialClientHttpTest` | 4 | 경로·키·corp/연도/보고서코드·연결우선 매핑·013·오류 (MockWebServer) |
| `FeatureCardPipelineServiceTest` | 4 | 태그 기록·실적 추세 기록(활성)·LLM·예외 격리 |

## 6. 잔여 (BACKLOG)

- **corp_code 리졸버 실구현**: DART corpCode.xml(전체 상장사) 다운로드·캐시 → stock_code→corp_code.
  이게 있어야 실적 추세가 실제로 채워진다.
- **DART 재무 실호출 검증**: 키 발급 후 실데이터로 계정명 매칭(매출액/영업이익 표기 변형)·분기 선택 튜닝 +
  픽스처 갱신. `financials-enabled=true` 활성화.
- **재무 조회 비용**: 현재 파이프라인(30분)에서 종목당 최대 8콜 가능 — 활성화 전 **분기 1회 갱신 잡**으로
  분리 검토(재무는 분기 단위로만 바뀜).
- **이슈 성격 LLM 보조**: 룰이 UNCLASSIFIED로 둔 애매 케이스를 LLM으로 재분류(3-E 재사용).
- **3-7b 수급 추세(flow_after, J6)**: 이번 슬라이스 범위 밖 — 다음.
