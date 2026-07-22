# 구현 보고서 — M3 3-E LLM 요약 클라이언트 (2026-07-22)

| 항목 | 내용 |
|------|------|
| 대상 | `domain-insight` — Why 카드의 what_happened를 LLM(Gemini)으로 생성 |
| 상태 | ✅ 배선·테스트 완료 (실호출 검증은 키 발급 후 잔여) |
| 브랜치 | `feat/insight-llm-summary` |
| 관련 | 플랜 M3 3-E (`docs/M3-domain-insight-plan.md`), 3-F 파이프라인(기존) |

---

## 1. 배경 — 무엇이 비어 있었나

J2 카드 파이프라인(3-F)은 이미 돌아가지만 **LLM(3-E)이 없어 what_happened가 템플릿 문장 전용**이었다
(`FeatureCardPipelineService` 주석: "LLM은 아직 미배선 → 템플릿 폴백, llm_used=false"). 파이프라인은
`llm_used` 플래그 + 템플릿 폴백 자리를 비워두고 LLM을 기다리는 구조였다 → 그 구멍을 채운다.

## 2. 구현 (플랜 3-E: 페이로드 조립 → Gemini → JSON 파싱 → 금지어 필터 → 템플릿 폴백 → 일 상한)

**순수 도메인** (프레임워크 무관, 단독 테스트)
- `BannedWordFilter` — 조언성 금지어(매수/매도/목표가/투자의견/손절…) 탐지. **오탐 방지**: `순매수`·`순매도`·
  `매수세` 등 수급 사실 표현은 스캔 전 제거(안 그러면 정상 카드가 매번 폴백됨).
- `WhyContext` — 3-B~3-D 결과(등락률·트리거·시장분해·수급·이슈)를 모은 사실 묶음.
- `WhyPromptComposer` — 사실만 근거로 초보자용 프롬프트 조립. 조언 금지·평문 2~3문장 규칙 명시.

**애플리케이션**
- `LlmGatewayPort`(out) — `generate(prompt)`, 오류·차단·빈 응답이면 예외 전파.
- `WhySummaryService` — **뉴스/공시 있는 종목만** → **일 상한 가드**(거래일 기준 인메모리 카운터) →
  LLM 호출 → 금지어 필터 → 성공 시 요약, 그 외 전부 `Optional.empty()`(파이프라인이 템플릿 폴백).

**어댑터**
- `GeminiClient`(`ExternalApiClient` 상속, `LlmGatewayPort` 구현) — `models/{model}:generateContent` 호출.
  `GeminiRequest`/`GeminiResponse` 레코드(`@JsonIgnoreProperties(ignoreUnknown)`로 안전등급 등 무시).

**배선**
- `FeatureCardPipelineService`: `whySummaryService.summarize(ctx)` → 성공 시 그 문장·`llm_used=true`,
  실패/비활성 시 시장분해 템플릿·`llm_used=false`.
- `domain-insight/build.gradle.kts`: `spring-web`·`spring-boot-starter-json`(RestClient/Jackson) +
  `mockwebserver:4.12.0`(test). 기존엔 도메인 최소화로 Jackson 미사용이었으나 외부 HTTP 어댑터에 필요.
- `app-collector/application.yml`: `signallink.llm.enabled`(기본 false) + `gemini.{base-url,api-key,model,pps}`.

## 3. 설계 결정

- **기본 off (`LLM_ENABLED=false`)**: Gemini 키 발급 전까지 기존 템플릿 동작을 그대로 유지. 키 준비 후
  환경변수만 켜면 활성화 → 코드 재배포 불필요.
- **이중 차단**: 프롬프트에 조언 금지 명시 + 출력 `BannedWordFilter` — 어느 한쪽이 뚫려도 조언성 문구 차단.
- **비용 가드 2단**: (1) 근거 이슈 없는 종목은 LLM 생략, (2) 거래일당 `daily-limit`(기본 60)회 상한.
- **응답은 평문**: 모델에 평문 2~3문장을 요청하고 응답 JSON 엔벨로프만 파싱 — 내부 JSON 스키마 결합 회피.
- **모델 ID `gemini-3-flash`(기본값·env override)**: 플랜 M3 표기. 실호출 검증 전이라 정확한 ID는 키 발급 시
  Google API 문서로 확인 필요(BACKLOG).

## 4. 검증 결과

`./gradlew build` — BUILD SUCCESSFUL. 신규/갱신 테스트 20건 전부 통과(스킵 0), 회귀 없음.

| 테스트 | 건수 | 커버 |
|--------|------|------|
| `BannedWordFilterTest` | 4 | 조언 탐지 · 수급 사실 오탐 방지 · null |
| `WhyPromptComposerTest` | 2 | 사실·조언금지 규칙 삽입 · 베타부족/이슈없음 표기 |
| `WhySummaryServiceTest` | 8 | 비활성·이슈없음·클린·금지어·예외·빈응답·상한·거래일 리셋 |
| `GeminiClientHttpTest` | 3 | 경로(`:generateContent`)·모델·키·바디 · 응답 파싱 · 후보없음 예외 |
| `FeatureCardPipelineServiceTest` | 3 | 폴백(llm_used=false) · LLM 성공(llm_used=true) · 예외 격리 |

`GeminiClientHttpTest`가 통과 → `models/test-model:generateContent`의 콜론이 인코딩되지 않고 그대로
전송됨을 확인(실 Gemini 경로와 일치).

## 5. 잔여 (BACKLOG)

- **Gemini 실호출 검증**: 키 발급 후 (1) 정확한 모델 ID 확정, (2) 실데이터 1건으로 프롬프트→출력 품질·
  금지어 필터 확인(플랜 M0/BACKLOG #4), (3) `LLM_ENABLED=true`로 스테이징 활성화.
- **일 상한 인메모리**: 단일 컬렉터 인스턴스 기준. 다중 인스턴스·재기동 누적이 필요하면 DB 카운트로 승격.
- **재요약 낭비**: 30분 배치마다 같은 종목을 재호출할 수 있음 — 필요 시 "당일 이미 llm_used=true면 재사용"
  최적화(현재는 상한 가드로 총량만 제한).
