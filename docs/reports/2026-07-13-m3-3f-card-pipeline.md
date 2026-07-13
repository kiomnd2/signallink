# M3 3-F J2 카드 생성 파이프라인 구현 보고

| 항목 | 내용 |
|------|------|
| 작성일 | 2026-07-13 |
| 범위 | Why 엔진 3-F — 3-A~3-D 산출물을 조립해 `feature_card` 자동 생성·갱신(J2 배치) |
| 결과 | ✅ 구현·테스트 완료 (단위 8건 그린 + 영속 2건 CI 검증), PR 대기 |

## 구현 내용

3-A(선정)·3-B(시장분해)·3-C(수급진단)·3-D(이슈매칭)를 한 파이프라인으로 엮어 카드를 만든다.

### domain-insight
- **`FeatureCard`**(JPA 엔티티): `insight.feature_card` 매핑. `(stock_code, trade_date)` upsert. source_refs는 `@JdbcTypeCode(SqlTypes.JSON)` String으로 jsonb 저장. created_at은 DB 기본값(now()).
- **`CardStatus`**(PUBLISHED·SUPPRESSED): 파이프라인 카드는 PUBLISHED.
- **`FeatureCardUpsert`**(포트 커맨드) + **`FeatureCardRepositoryPort`**: source_refs는 도메인 `IssueRef` 리스트로 넘기고 jsonb 직렬화는 어댑터가 담당(애플리케이션 기술 의존 0).
- **`SourceRefsJson`**(어댑터 유틸): `IssueRef` → jsonb 문자열 **수동 직렬화**(common.DiscordNotifier와 동일 사유 — Jackson 의존 회피, 이스케이프 처리).
- **`FeatureCardPersistenceAdapter`** + **`FeatureCardJpaRepository`**: `(stock,date)` upsert.
- **`FeatureCardPipelineService`**: `select → for each: analyze/diagnose/match → upsert`, **종목 단위 예외 격리**. LLM 전이라 what_happened=템플릿 폴백, `llm_used=false`.

### app-collector
- **`FeatureCardJob`**(J2): 장중 30분(`0 5/30 9-15 * * MON-FRI`, KST). 배치 자체 실패는 `DiscordNotifier.jobFailed`로 알림 → 30분 뒤 멱등 재실행 회복.
- **`NotificationConfig`**: `DiscordNotifier` 빈(webhook-url 비면 no-op).
- `application.yml`: `signallink.discord.webhook-url`, `signallink.feature-card.cron`.

## 주요 결정
- **jsonb 매핑**: 미리 만든 JSON 문자열을 `@JdbcTypeCode(SqlTypes.JSON)` String으로 저장(이중 인코딩 없음). 실 DB 왕복은 Testcontainers 영속 테스트로 CI 검증(로컬 Docker 미가동 시 skip).
- **직렬화 위치**: 애플리케이션은 프레임워크 의존 최소 원칙에 따라 도메인 `IssueRef`까지만 다루고, jsonb 문자열화는 어댑터에서.
- **status/llm_used**: 템플릿 폴백 카드는 `PUBLISHED` + `llm_used=false`. 3-E 배선 시 LLM 성공분만 `llm_used=true`, 조언성 감지분은 `SUPPRESSED`로 확장.
- **cron 09~15 5/30**: 선정·시세 데이터가 장중 갱신되므로 30분 주기. 시세 확정 배치(16:30)·수급 확정(16:40) 이후 마감 카드 보정은 후속(운영 튜닝).

## 검증
- `FeatureCardPipelineServiceTest`(2): 다종목 조립 upsert · **종목 단위 예외 격리**
- `SourceRefsJsonTest`(4): 빈 배열·필드 직렬화·이스케이프·다건
- `FeatureCardPersistenceTest`(2, CI): source_refs jsonb 왕복 · `(stock,date)` upsert 갱신
- `./gradlew :domain-insight:test :app-collector:build` + 전체 컴파일 그린

## M3 현황 / 잔여
- 3-A~3-D + **3-F 완료** → 카드가 LLM 없이도 템플릿으로 자동 생성됨(플랜상 "3-E까지면 M4 진행" 기준 근접).
- **3-E LLM 클라이언트**(Gemini·금지어 필터·폴백·일 상한) — Gemini 키 준비 시. 배선점은 파이프라인의 what_happened 생성부.
- 3-G health_check · 3-H macro — 후속(M4 병행).
