# SignalLink Backend

주식 초보자용 시황 번역 대시보드 — 백엔드 (기획확정서 v2.5 / 상세스펙 v1.0 / 구현플랜 v1.0 기준)

## 구조 (모듈러 모놀리스)

```
common/          공통 유틸·응답 포맷
domain-market/   시세·수급          [KIS 키 소유 예정]
domain-issue/    공시·뉴스
domain-macro/    지표·신호등·이벤트  (→ market 읽기)
domain-insight/  특징주·Why 엔진    (→ market, issue, macro 읽기) [LLM 키 소유 예정]
domain-content/  용어사전·도미노 룰  (독립)
app-collector/   스케줄러 배포 단위 (:8081)
app-api/         REST 배포 단위    (:8080, Flyway 마이그레이션 소유)
```

의존 규칙은 Gradle 모듈 의존으로 강제된다 — 역방향 참조는 컴파일 에러.

## 시작하기

요구사항: JDK 21, Gradle 8.12+ (또는 Docker만으로도 가능)

```bash
# 최초 1회: 래퍼 생성 (이후 ./gradlew 사용)
gradle wrapper --gradle-version 8.12

# 전체 빌드
./gradlew build

# 로컬 전체 기동 (DB + api + collector)
docker compose up --build
# 확인: http://localhost:8080/api/v1/ping , /actuator/health
```

## 다음 작업 (구현플랜 M1 잔여)

- [ ] GitHub repo 연결, CI 통과 확인
- [ ] VPS 배포 워크플로 추가 (ssh + compose pull&up)
- [ ] common: 외부 API 클라이언트 베이스(rate limiter·백오프) + Discord 알림
- [ ] Uptime Kuma 헬스체크 연결
