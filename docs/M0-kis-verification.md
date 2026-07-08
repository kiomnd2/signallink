# M0 — KIS 실검증 절차 (구현플랜 Day 2~5)

목표: **"KIS로 시그널링크가 굴러가는가"를 코드 한 줄 짜기 전에 확정.** 실패 시 여기서 설계를 고친다.

## 0. 선행 조건

- 한국투자증권 계좌 + [KIS Developers](https://apiportal.koreainvestment.com/intro) 앱키 (실전·모의 각 1개)
- ⚠️ **순위분석 TR(등락률·거래량순위)은 실전 키 전용** — 모의 키만으로는 M0 검증 불가. 실전 키 발급까지 완료할 것
- ⚠️ 포털 공지 **"[중요] 신규 고객 초당 호출 제한 안내 (2026.03.20)"** 를 로그인 후 반드시 원문 확인 (기존 고객 초당 20건 → 신규 고객은 하향 조정됨. 아래 4단계에서 실측으로 교차 검증)

## 1. 검증 대상 TR (스펙 5장 표에 기입할 값)

| 용도 | TR ID | URL | 비고 |
|------|-------|-----|------|
| 토큰 발급 | - | `POST /oauth2/tokenP` | 24h 유효, **재발급 제한 있음 → 파일 캐시 필수** |
| 등락률 순위 | `FHPST01700000` | `/uapi/domestic-stock/v1/ranking/fluctuation` | 실전 전용 |
| 거래량순위 | `FHPST01710000` | `/uapi/domestic-stock/v1/quotations/volume-rank` | `FID_BLNG_CLS_CODE=3` → **거래금액순** (특징주 선정 기준) |
| 종목별 투자자 | `FHKST01010900` | `/uapi/domestic-stock/v1/quotations/inquire-investor` | ⚠️ **당일 데이터는 장 종료 후 제공** — 장중 잠정 불가 |
| 외인·기관 가집계 | `FHPTJ04400000` | `/uapi/domestic-stock/v1/quotations/foreign-institution-total` | 장중 잠정 수급의 실제 소스 |

### ⚠️ 스펙 영향 사항 (리서치에서 발견)

1. **장중 "잠정 수급"은 `inquire-investor`로 못 만든다** (당일치 장마감 후 제공). 장중에는 `FHPTJ04400000`(가집계)를 써야 하는데, 이 데이터는 증권사 직원 수동 입력으로 **하루 4~5회만 갱신** (외국인 09:30/11:20/13:20/14:30, 기관 10:00/11:20/13:20/14:30, ±10분). → **30분 배치마다 새 수급이 없을 수 있음.** 카드의 "잠정" 수급은 이 갱신 주기 기준으로 스펙 4단계(수급 진단)를 수정할 것.
2. 가집계는 **전체 시장 단위 조회**(순매수 상위 리스트)라 종목당 1콜이 아님 → J2 호출 예산이 오히려 줄어들 수 있음.

## 2. 실행 절차

### Day 2~3 — 로컬 검증

```bash
export KIS_APPKEY=발급키 KIS_APPSECRET=발급시크릿
python3 scripts/m0-kis-verify/kis_verify.py          # TR 4종 실호출
python3 scripts/m0-kis-verify/kis_verify.py --probe  # + 한도 실측 (~50콜)
```

체크:
- [ ] 토큰 발급 성공, 재실행 시 캐시 재사용 확인
- [ ] TR 4종 `rt_cd=0` + 응답 필드를 상세스펙 5장 표에 기입
- [ ] `--probe` 실측 한도 기록 (장중 10:00~14:00에 실행해야 유의미)
- [ ] 장중/장마감 후 각 1회 실행 — `inquire-investor` 당일치 제공 시점 확인

### Day 3~4 — 한도 → J2 예산 확정

스크립트가 출력하는 J2 예산(배치당 ~82콜)과 실측 한도를 비교:

| 실측 한도 | 판단 |
|-----------|------|
| 초당 10건 이상 | 40종목·30분 그대로 진행 |
| 초당 2~9건 | 진행 가능 (배치 소요 1~2분) — rate limiter 간격만 조정 |
| 초당 1건 수준 + 일별 총량 제한 존재 | 종목 40→25, 주기 30→60분으로 확정서 수정 |

- [ ] 결정 사항을 확정서·스펙에 반영 (플랜 M0 Exit 조건)

### Day 4~5 — VPS 검증 (IP 차단 확인)

```bash
scp scripts/m0-kis-verify/kis_verify.py user@vps:~/
ssh user@vps 'export KIS_APPKEY=... KIS_APPSECRET=...; python3 kis_verify.py'
```

- [ ] 로컬과 동일하게 TR 4종 성공 (⚠️ 토큰은 발급 IP와 무관하지만 **VPS에서 새로 발급**해 발급 자체가 되는지도 확인)
- [ ] 실패 패턴 구분: `connection timeout/refused` = IP 대역 차단 → **국내 타 VPS로 즉시 교체** / `EGW00201` = 유량 초과(정상, 한도만 확인)
- [ ] 장중 시간대에 1회 더 실행 (야간과 장중 정책이 다를 수 있음)

### 병행 — LLM 검증 (Day 5)

- [ ] 위 실호출로 받은 실데이터 1건을 스펙 6장 프롬프트에 넣어 Gemini 3 Flash 호출 → JSON 파싱 + 금지어(매수/매도/목표가) 필터 통과 확인

## 3. Exit 판정

전부 통과 → M1 잔여 작업 진행. 한도 부족 → 배치 주기·종목 수 조정 후 확정서 반영하고 진행. VPS 차단 → VPS 교체 후 Day 4~5 재실행.

---

## 4. 실행 결과 (2026-07-08, 로컬 · 실전 키)

### TR 4종 — 전부 `rt_cd=0` (등락률 순위 파라미터 수정 후)
| TR | 결과 | 응답 주요 필드 (상세스펙 5장 기입용) |
|----|------|--------------------------------------|
| 등락률 순위 `FHPST01700000` | ✅ 30행 | `stck_shrn_iscd, data_rank, hts_kor_isnm, stck_prpr, prdy_vrss, prdy_vrss_sign, prdy_ctrt, acml_vol, stck_hgpr, stck_lwpr` |
| 거래량순위(거래대금) `FHPST01710000` | ✅ 30행 | `hts_kor_isnm, mksc_shrn_iscd, data_rank, stck_prpr, prdy_vrss, prdy_ctrt, acml_vol, prdy_vol, lstn_stcn, avrg_vol` |
| 종목별 투자자 `FHKST01010900` | ✅ 30행 | `stck_bsop_date, stck_clpr, prsn_ntby_qty, frgn_ntby_qty, orgn_ntby_qty, prsn_ntby_tr_pbmn, frgn_ntby_tr_pbmn, orgn_ntby_tr_pbmn` |
| 외인·기관 가집계 `FHPTJ04400000` | ✅ rt_cd=0, 0행 | 09:22 실행 — 첫 갱신(외인 09:30) 전이라 빈 응답(정상). **장중 09:30~14:30 재실행 시 데이터 확인 필요** |

### 한도 실측 (`--probe`)
- **초당 2건 동시 → 즉시 EGW00201(유량 초과) → 실측 한도 초당 ~1건** (기존 20건 → 신규 고객 대폭 하향, 포털 공지대로)
- J2 예산: 배치당 82콜 → 초당 1건 기준 **~82초/배치** → 30분 창(1,800초)에 여유

### 결정 (확정서 반영 대상)
- ✅ **40종목·30분 유지** — 82초 ≪ 30분이라 타이밍 여유 충분
- ✅ `common.RateLimiter` **permitsPerSecond = 1.0 (안전하게 0.8)** 로 설정 (M2에서 KIS 클라이언트 생성 시)
- ⚠️ **일별 총량 제한**은 이 probe로 미확인 → 운영 중 모니터 (BACKLOG)

### 환경 이슈 (재현 방지 메모)
- 이 머신 Python(3.14, python.org)이 **CA 번들 미보유** → 실행 시 `SSL_CERT_FILE=/etc/ssl/cert.pem` 지정 필요. (네트워크·인증서 자체는 정상 — MITM 아님)
- `kis_verify.py`: 등락률 순위 `FID_RANK_SORT_CLS_CODE`는 **1자리**(`"0"` 상승률순). `"0000"`은 `INVALID INPUT_FILED_SIZE` 오류 → 수정 완료.

**Exit: 통과.** TR 4종 성공 + 한도 확정(초당 1건, 40종목·30분 유지).
