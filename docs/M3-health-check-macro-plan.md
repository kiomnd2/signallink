# M3 잔여 구현 플랜 — 체력 진단(3-7 health_check) · 거시(3-8 macro)

| 항목 | 내용 |
|------|------|
| 작성일 | 2026-07-22 |
| 기준 | 구현플랜 M3 7·8번 · 기획확정서 v2.5(체력 진단 경량판) · `docs/M3-domain-insight-plan.md` |
| 위치 | Why 엔진 핵심(3-A~3-F) 완료 후 잔여. **M4 착수를 막지 않는 병행 후속** |
| 성격 | 카드에 "이 악재 오래 갈까?" 판단 재료(체력 진단 3종 팩트) + 거시 이벤트 반응 이력 |

---

## 0. 현재 상태

- **domain-insight**: 3-A~3-F(선정·시장분해·수급·이슈매칭·LLM·파이프라인) 완료. health_check 코드 **없음**.
- **domain-macro**: `package-info.java`만 있는 **빈 모듈** — 엔티티부터 신규.
- 데이터 기반: market 2년 백필(M2) 완료 → 과거 재현 검증 가능.

## 1. 대상 스키마 (기존 V1)

```
insight.health_check(card_id UNIQUE→feature_card, issue_nature NOT NULL,
                     revenue_trend, profit_trend, flow_after jsonb, updated_at)
macro.macro_event(event_type, occurred_on, release_id→indicator_release(nullable), description,
                  UNIQUE(event_type, occurred_on))
macro.asset_reaction(event_id→macro_event, asset_code, d1_return, d7_return, d30_return,
                     UNIQUE(event_id, asset_code))
```

체력 진단 3종 팩트(기획확정서 §체력 진단 경량판) → health_check 매핑:
1. **이슈 성격 태그**(`issue_nature`): 일회성 사건 vs 구조적 문제, 애매하면 "분류 불가". 룰 + LLM 보조.
2. **실적 추세**(`revenue_trend`/`profit_trend`): DART 재무로 최근 4분기 매출·영업이익 방향(개선/유지/악화).
3. **수급 추세**(`flow_after`): 이슈 후 D+1~ 외국인·기관 매도 지속/중단 여부(자동 추적).
> 출력 원칙: **팩트만**, 결론(보유·매도 권유) 생성 금지 — Why 카드와 동일 차단 규칙(`BannedWordFilter` 재사용).

## 2. 선결 의존성·제약 (2026-07-22 조사)

| # | 발견 | 대응 |
|---|------|------|
| 1 | `FeatureCardRepositoryPort.upsert()`가 **void** | health_check는 `card_id` 필요 → **upsert가 id 반환**하도록 수정(또는 `(stock,date)→id` 조회 포트) |
| 2 | issue 도메인에 **DART 재무제표 소스 없음**(공시 list·뉴스만) | 실적 추세는 **DART 재무 API 신규 어댑터** 필요 → 3-7 중 가장 무거움 |
| 3 | market `InvestorFlowQueryPort`·`DailyPrice/IndexPriceQueryPort` **존재** | flow_after·asset_reaction은 **기존 포트로 지금 계산 가능**(백필 활용) |
| 4 | `macro_event.release_id` → `indicator_release`는 **M4(J4 신호등)가 채움** | macro_event **라이브 기록은 M4와 묶기**. `release_id` nullable → **과거 이벤트 백필은 시드로 독립 가능** |
| 5 | 결론 문장 금지(팩트만) | 기존 `BannedWordFilter`·LLM(3-E) 재사용 |

---

## 3. 서브슬라이스 (구현 순서)

### 3-7 health_check — 비용 순

#### 3-7a · 이슈 성격 태그 🟢 지금 가능 (먼저)
- **입력**: 기존 `IssueMatch`/`source_refs`(공시 카테고리·뉴스 제목).
- **신규**: `IssueNatureClassifier`(순수, 공시 카테고리·키워드 룰 → 일회성/구조적/분류불가) + LLM 보조(3-E 재사용) +
  `HealthCheckUpsert`/`HealthCheckRepositoryPort`/영속 어댑터. **제약 #1 해결**(upsert→card_id).
- **배선**: J2 파이프라인에서 카드 upsert 직후 health_check 기록.

#### 3-7b · 수급 추세 flow_after (J6) 🟢 지금 가능
- **입력**: market `InvestorFlowQueryPort`.
- **신규**: `FlowAfterTracker`(순수, D+1~D+N 외국인·기관 순매수 방향 판정 → 매도지속/중단/전환) +
  **J6 스케줄 잡**(app-collector, 마감 후 D+1~D+5 갱신) + `flow_after` jsonb 직렬화(`SourceRefsJson` 패턴).
- health_check가 이미 있는 카드를 대상으로 갱신 → `(stock,date)→card_id` 조회 필요(제약 #1과 동일 기반).

#### 3-7c · 실적 추세 (DART 재무) 🟡 신규 외부 소스 (가장 뒤/독립 슬라이스)
- **신규**: issue 도메인에 **DART 재무제표 API 어댑터**(예: 단일회사 주요계정 `fnlttSinglAcnt`) +
  `FinancialTrendPort` + `TrendCalculator`(순수, 최근 4분기 매출·영업이익 방향). MockWebServer HTTP 테스트(DART 패턴).
- **완충**: 미구현/미매칭 시 `revenue_trend=null`("데이터 없음") → 카드 깨지지 않음. Exit 완결 전까지 뒤로 미뤄도 무방.

### 3-8 macro — 빈 모듈부터

#### 3-8a · macro 모듈 골격 🟢 지금 가능
- 엔티티 `MacroEvent`·`AssetReaction`(도메인 모델 겸용, Level 1) + JPA 영속 어댑터 + 포트. macro→market 읽기 의존 기선언.

#### 3-8b · asset_reaction 계산 (J5) + 2년 백필 🟢 지금 가능
- **입력**: market `DailyPriceQueryPort`/`IndexPriceQueryPort` + **M2 2년 백필**(완료) → 과거 이벤트로 즉시 검증.
- **신규**: `AssetReactionCalculator`(순수, 이벤트일 대비 자산 D+1/D+7/D+30 등락률) + J5 잡 +
  **과거 이벤트 시드 백필 스크립트**(FOMC·CPI 등 알려진 날짜·자산 목록).

#### 3-8c · macro_event 라이브 기록 🔴 M4와 묶기
- 신호등 판정(J4) 시 `indicator_release` → `macro_event` 자동 기록. **M4 J4에 의존 → M4 신호등과 함께 구현.**

---

## 4. 권장 순서

```
지금 (기존 데이터로 완결 가능):
  3-7a 이슈 성격 태그  →  3-7b flow_after(J6)  →  3-8a macro 골격  →  3-8b asset_reaction(J5)+백필

무거움/의존 있음 (뒤로 또는 M4와):
  3-7c DART 재무추세 (신규 외부 소스 — 독립 슬라이스로 언제든)
  3-8c macro_event 라이브 기록 (M4 J4 신호등과 묶기)
```

**착수 추천**: 데이터가 이미 있고 가장 저렴한 **3-7a(이슈 성격 태그)**. 여기서 `upsert→card_id` +
`HealthCheckRepositoryPort` 기반이 서면 3-7b가 그 위에 얹힌다.

## 5. 테스트 전략

- 순수 판정(`IssueNatureClassifier`·`FlowAfterTracker`·`TrendCalculator`·`AssetReactionCalculator`)은 단독 단위 테스트.
- **백필 픽스처로 과거 재현**: 유명 급등락/거시 이벤트로 태그·추세·반응이 맞게 나오는지.
- DART 재무 어댑터는 MockWebServer HTTP 경로 테스트(실키 불필요, CI 상시 실행) — 실호출 검증은 키 발급 후(BACKLOG).
- 영속(`@DataJpaTest`+Testcontainers)은 health_check FK·macro UNIQUE 제약 검증.

## 6. Exit

- 특징주 카드에 **체력 진단 3종 팩트(이슈 태그·실적·수급 추세)** 정상 표시(기획확정서 DoD 2-1).
- 과거 거시 이벤트 3건에 대해 asset_reaction(D+1/7/30)이 백필로 재현.
- (3-7c·3-8c는 각각 DART 키·M4 J4 준비 시 완결 — 그 전까지 "데이터 없음"으로 우아하게 비표시)
