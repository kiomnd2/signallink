# 아키텍처 컨벤션 — 헥사고날 (Level 1, 실용형)

| 항목 | 내용 |
|------|------|
| 확정일 | 2026-07-07 |
| 결정 | 모듈러 모놀리스 위에 **각 `domain-X` 모듈 내부를 포트&어댑터(헥사고날) 패키지 구조**로 |
| 깊이 | **Level 1 (실용)** — 포트/어댑터는 도입, JPA 엔티티를 도메인 모델로 겸용(매퍼 없음) |
| 이유 | 1인·13~15주·"완벽주의 금지" 원칙. 교체성·테스트 이득의 대부분을 저비용으로 확보 |

---

## 1. 모듈 vs 패키지

- **Gradle 모듈 구조는 그대로** (`common` / `domain-{market,issue,macro,insight,content}` / `app-{collector,api}`). 레이어별 모듈 분리는 하지 않는다.
- 도메인 의존 방향은 **기존대로 Gradle 모듈 의존으로 강제** (`insight → market·issue·macro`, `macro → market`, `content` 독립, 역참조는 컴파일 에러).
- **헥사고날은 각 도메인 모듈 "내부 패키지"** 로 표현한다.

## 2. 도메인 모듈 내부 표준 패키지 (`io.signallink.<domain>`)

```
io.signallink.<domain>
├─ domain/                도메인 모델(@Entity 겸용) + 도메인 규칙/서비스
├─ application/
│  ├─ port/
│  │  ├─ in/              인바운드 포트 = 유스케이스 인터페이스   (선택: §5)
│  │  └─ out/             아웃바운드 포트 인터페이스 (Repository·Gateway·Watchlist…)  (필수)
│  └─ service/            유스케이스 구현 (@Service, 포트에만 의존)
└─ adapter/
   └─ out/
      ├─ persistence/     Spring Data JpaRepository + <X>PersistenceAdapter (impl out 포트)
      └─ external/        <X>Client extends ExternalApiClient (impl gateway 포트)
```

**인바운드 어댑터는 app 모듈에 위치** (도메인 모듈의 `port/in`을 호출):
- REST 컨트롤러 → `app-api` (`io.signallink.api.*`)
- 스케줄러 잡 → `app-collector` (`io.signallink.collector.job.*`)

## 3. 의존 규칙

- `domain/`·`application/` 은 **프레임워크 의존 최소**. 외부 기술(DB·HTTP·다른 도메인 데이터)은 **반드시 `port/out` 인터페이스 경유**.
- 프레임워크/외부 라이브러리에 대한 의존은 **`adapter/`에만** 둔다.
- 유스케이스(`service`)는 구체 어댑터를 모른다 — 포트 인터페이스만 주입받는다(생성자 주입).
- **도메인 간 참조는 ID만** (기존 규칙 유지). 타 도메인 데이터가 필요하면 그 값을 넘겨받거나 `port/out`으로 정의하고, 조합은 상위(app 또는 insight)에서.

## 4. Level 1 절충 (순수형과 다른 점)

- **엔티티 = 도메인 모델 겸용**: `domain/`의 클래스에 JPA 애노테이션(`@Entity`, 스키마 지정)을 허용한다. 순수 POJO + 별도 JpaEntity + 매퍼는 두지 않는다. → `Stock.java` 형태 유지, 위치만 `domain/`.
- **매퍼 계층 없음**. 외부 API 응답 DTO → 도메인 변환은 어댑터 내부 메서드로 간단히.

## 5. 인바운드 포트(port/in)는 선택

- 진입점이 하나뿐인 단순 수집 잡 등은 `port/in` 없이 `application/service`의 `@Service`를 인바운드 어댑터가 직접 호출해도 된다(실용).
- 여러 진입점이 쓰거나 계약을 명시·테스트하고 싶은 유스케이스(예: 조회 API가 여러 곳에서 재사용)는 `port/in` 인터페이스로 노출한다.
- **`port/out`(아웃바운드 포트)은 항상 필수** — 여기서 교체성·모킹 이득이 나온다(예: 뉴스 워치리스트 소스 교체, 외부 API 스텁).

## 6. 네이밍

| 대상 | 접미사 | 예 |
|------|--------|-----|
| 인바운드 포트(유스케이스) | `UseCase` | `CollectDisclosuresUseCase` |
| 아웃바운드 포트 | `Port` | `DisclosureRepositoryPort`, `DartGatewayPort`, `WatchlistPort` |
| 유스케이스 구현 | `Service` | `DisclosureCollectService` (또는 단순히 `DisclosureCollector`) |
| 영속 어댑터 | `PersistenceAdapter` | `DisclosurePersistenceAdapter` |
| 외부 API 어댑터 | `Client` | `DartClient`, `NaverNewsClient` |

## 7. 기존 코드 정리 메모

- `domain-market`의 `Stock.java`(@Entity)·`StockRepository`는 **예시/미구현** 상태. market 실구현(KIS 이후) 시 위 구조로 재배치: `Stock`→`domain/`, `StockRepository`(Spring Data)→`adapter/out/persistence/` + `StockRepositoryPort`(application/port/out) + 어댑터.
- `common`의 `ExternalApiClient`는 **아웃바운드 어댑터의 공통 베이스**로 유지(각 도메인 external 어댑터가 상속).
- 이 구조의 첫 레퍼런스 구현은 **domain-issue** (`docs/M2-domain-issue-plan.md`).
