# M3 3-D 이슈 매칭 구현 보고

| 항목 | 내용 |
|------|------|
| 작성일 | 2026-07-13 |
| 범위 | Why 엔진 3-D — 급등락의 근거(공시·뉴스)를 시간창으로 매칭해 후보 정리 |
| 결과 | ✅ 구현·테스트 완료 (단위 7건 그린), PR 대기 |

## 구현 내용

`insight → issue` 읽기는 3-B/3-C와 동일하게 **issue가 소유한 조회 포트**를 통한다.

### domain-issue (공시·뉴스 소유)
- `IssueQueryPort`(신규): `disclosures(stock, from, to)` / `news(stock, from, to)` — 시간창 조회
- `DisclosureRef`·`NewsRef`(신규 읽기 DTO)
- JPA 파생 쿼리: `findByStockCodeAndDisclosedAtBetween` / `findByStockCodeAndPublishedAtBetween`
- `IssueQueryPersistenceAdapter`(신규): 두 JPA 리포지토리를 묶어 포트 구현

### domain-insight (Why 엔진)
- `IssueMatcher`(순수 도메인): 근거 후보 정리
  - **공시 우선** — 공시(사실)가 뉴스(해석)보다 항상 앞
  - 같은 종류 안에서는 **최신순**(발생 시각 내림차순)
  - 합쳐서 **최대 3건**(`MAX_REFS`)으로 컷
- `IssueRef`(type·title·url·occurredAt·detail) / `IssueType`(DISCLOSURE·NEWS) / `IssueMatch`(refs + primary())
- `IssueMatchService`(유스케이스): 거래일 기준 **KST 하루 시간창** 계산 → 포트 조회 → issue DTO를 `IssueRef`로 매핑 → `IssueMatcher` 위임

## 주요 결정
- **공시 우선**: 공시는 규제 사실, 뉴스는 해석·추측이라 근거 신뢰도가 높음 → 정렬에서 항상 앞.
- **시간창**: 거래일의 KST 00:00~23:59로 시작. 전일 야간 뉴스 포함 등 확장은 3-F 배선/검증에서 조정.
- **jsonb 직렬화 분리**: 3-D는 `IssueMatch`(도메인)까지만. `feature_card.source_refs`(jsonb) 직렬화·저장은 3-F J2 파이프라인에서(3-B `MarketAnalysis`·3-C `FlowDiagnosis`와 함께 카드 조립).
- **detail 필드**: 공시=카테고리, 뉴스=출처를 담아 카드/근거 표시에 재사용.

## 검증
- `IssueMatcherTest`(4): 공시 우선·최신순·3건 컷·빈 결과
- `IssueMatchServiceTest`(3): 공시/뉴스 매핑·KST 하루 시간창·빈 매칭
- `./gradlew :domain-issue:test :domain-insight:test` + 전체 컴파일 그린

## 잔여
- 3-E LLM 클라이언트(Gemini·금지어 필터·폴백·일 상한) — Gemini 키 준비 시
- 3-F J2 파이프라인: 3-A~3-D 산출물(`MarketAnalysis`·`FlowDiagnosis`·`IssueMatch`)을 `feature_card`로 upsert(source_refs jsonb 직렬화 포함), 종목 단위 예외 격리 + Discord 실패 알림
