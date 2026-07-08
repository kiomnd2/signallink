# 구현 진행 종합 보고서 (2026-07-08)

| 항목 | 내용 |
|------|------|
| 기준 | 기획확정서 v2.5 · 구현플랜 v1.0 · `docs/1.이후-구현플랜.md`(M0~M8) |
| 범위 | M1 마무리 · M0 검증 · 아키텍처/컨벤션 · domain-issue(공시·뉴스) · domain-market(슬라이스 1~2) |
| 아키텍처 | 모듈러 모놀리스 + 헥사고날 Level 1 (`docs/ARCHITECTURE.md`) |
| 병합 PR | #1~#13 (전부 CI green 후 머지) |

---

## 1. 완료 요약

| 영역 | 상태 | 관련 PR |
|------|------|---------|
| M1 골격(CI) | ✅ 빌드/테스트 CI 통과 | #1 |
| M0 KIS 실검증 | ✅ TR 검증·한도(초당 1건)·정책 감사 | #4 |
| 아키텍처·컨벤션 | ✅ 헥사고날·Lombok·상수·CLAUDE.md·문서 | #2·#5·#6·#7 |
| **domain-issue** | ✅ 공시(DART) + 뉴스(네이버) | #3·#8·#9 |
| **domain-market** | ✅ 슬라이스1(마스터·토큰) + 슬라이스2(시세·지수·백필·무인수집) | #7·#8·#10~#13 |

---

## 2. 영역별 상세

### M1 골격
- Gradle 9 환경에서 테스트 실행 실패(JUnit launcher 누락) 수정 → CI green (#1).

### M0 KIS 실검증 (`docs/M0-kis-verification.md §4`)
- 토큰 발급 + TR 4종(등락률·거래량 순위, 종목별 투자자, 외인·기관 가집계) `rt_cd=0` 확인.
- **호출 한도 실측 = 초당 ~1건**(신규 고객 하향) → `RateLimiter(1.0)` 확정.
- 이후 일별시세(FHKST03010100)·지수(FHKUP03500100)·종목마스터(.mst)도 실검증.

### 아키텍처·컨벤션
- **헥사고날 Level 1**: 도메인 모듈마다 `domain / application(port·service) / adapter.out(persistence·external)`. 엔티티=모델 겸용.
- **Lombok**: `@RequiredArgsConstructor`(DI)·`@Getter`(엔티티). 외부 status 코드는 상수 클래스(`DartStatus`).
- **CLAUDE.md**: 협업 규칙(브랜치→PR·gitmoji 커밋·문서 동기화·**구현 보고서 매번 작성**) + 함정(Gradle9·`out/` gitignore·Testcontainers·SSL).
- **문서**: `ARCHITECTURE.md`, 도메인 정의서(5 바운디드 컨텍스트), 도메인별 플랜, 구현 보고서.

### domain-issue — 공시·뉴스
- **공시(DART, J3-A)**: `DartClient`(list.json) → 상장사·신규(rcept_no) 필터 → 카테고리 분류 → 저장. 건별 예외 격리, 비정상 status 예외화.
- **뉴스(네이버, J3-B)**: `NaverNewsClient`(RateLimiter) → HTML 태그·엔티티 제거 → (stock_code,url) 중복 제거. 워치리스트=시드 CSV 15종목(추후 특징주로 교체).

### domain-market — 슬라이스 1~2
- **1-A 파운데이션**: Stock 헥사고날 + **KIS 토큰 인프라**(`KisTokenProvider` DB 캐시, 만료 10분 전 재사용).
- **1-B 종목마스터(J1)**: `.mst`(cp949 288B) 파싱 → 주식(ST)·6자리 필터 → market.stock upsert.
- **2-① 영속**: DailyPrice·IndexPrice(복합키) 엔티티·포트·어댑터.
- **2-② 수집**: `KisMarketClient`(일별시세·지수, RateLimiter 1.0) + **등락률 계산**(응답에 없어 전일대비/직전종가로 산출) + 멱등 수집.
- **2-③ 백필**: 2년치를 100일 창 분할(정책③)·상위 300·멱등·재개. `BackfillRunner`(수동).
- **2-④ 무인 수집**: `MarketCollectJob`(@Scheduled 평일 16:30 KST) — J1 sync + 일일 시세. → **슬라이스 2 Exit 달성**.

---

## 3. 검증 상태
- 모든 PR **CI(build + GitGuardian) green** 후 머지.
- 테스트: 도메인 규칙·파서(픽스처)·수집 서비스(포트 스텁) **단위 테스트** + **Testcontainers Postgres** 영속 테스트(로컬 Docker 이슈로 skip, CI 실행).
- KIS: 토큰·순위·수급·일별시세·지수·종목마스터 **실호출 검증 완료**.

## 4. 주요 결정·사고 (교훈)
- **등락률**: KIS 일별시세/지수 응답에 등락률 필드 없음 → 전일대비·직전종가로 계산(실검증으로 발견, 추측 회피).
- **`.env` 따옴표**: 값이 `"..."`로 감싸여 수동 파싱 시 EGW00103(유효하지 않은 AppKey) 유발. 셸 `source`는 벗기지만 일부 로더는 아님 → 따옴표 제거로 해결(“토큰 발급 제한/차단”은 오진이었음).
- **PR #7 누락 복구**: PR #7이 후속 커밋(J1)을 누락 → 커밋이 살아있어 cherry-pick으로 복구(#8).
- **토큰 캐시**: 절대 삭제 금지(재발급 제한). 앱은 DB 캐시로 자동 방지.

## 5. 남은 로드맵
- **domain-market**: vol_ratio_20d 계산 · 슬라이스3(investor_flow 수급) · 슬라이스4(stock_beta J7) · 실호출 dry-run
- **domain-issue**: J3 스케줄러 배선 · 실호출 dry-run(OpenDART·네이버 키)
- **이후 마일스톤**: M3 Why 엔진(최다 리스크) → M4 API+신호등 → M5 프론트 → M6 도미노·번역기 → M7 QA → M8 런칭
- **인프라(M0/M1 잔여)**: VPS 계약 → deploy 활성·Uptime Kuma·VPS IP 차단 검증, LLM(Gemini) 품질 검증, 나머지 키(FRED·ECOS·KOSIS)
