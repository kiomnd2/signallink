#!/usr/bin/env python3
"""M0 KIS 실검증 스크립트 — 표준 라이브러리만 사용 (VPS에서 pip 없이 실행 가능).

사용법:
  export KIS_APPKEY=... KIS_APPSECRET=...
  python3 kis_verify.py            # 토큰 + TR 4종 실호출
  python3 kis_verify.py --probe    # + 초당 호출 한도 실측 (호출 ~50건 소모)

주의: 순위분석 TR(등락률·거래량순위)은 실전 키 전용. 모의(KIS_ENV=demo)에서는 실패가 정상.
"""
import json
import os
import sys
import time
import urllib.request
import urllib.error
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

ENV = os.environ.get("KIS_ENV", "real")
BASE = {
    "real": "https://openapi.koreainvestment.com:9443",
    "demo": "https://openapivts.koreainvestment.com:29443",
}[ENV]
APPKEY = os.environ.get("KIS_APPKEY", "")
APPSECRET = os.environ.get("KIS_APPSECRET", "")
TOKEN_CACHE = Path.home() / ".kis_token.json"

RESULTS = []


def http(method, path, headers=None, body=None, params=None, timeout=10):
    url = BASE + path
    if params:
        from urllib.parse import urlencode
        url += "?" + urlencode(params)
    req = urllib.request.Request(url, method=method)
    req.add_header("content-type", "application/json; charset=utf-8")
    for k, v in (headers or {}).items():
        req.add_header(k, v)
    data = json.dumps(body).encode() if body else None
    try:
        with urllib.request.urlopen(req, data=data, timeout=timeout) as r:
            return r.status, json.loads(r.read().decode())
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read().decode())
        except Exception:
            return e.code, {}
    except Exception as e:
        return 0, {"error": str(e)}


def get_token():
    """토큰 캐시(23h) — tokenP는 재발급 제한이 있으므로 반드시 재사용."""
    if TOKEN_CACHE.exists():
        c = json.loads(TOKEN_CACHE.read_text())
        if time.time() - c["issued_at"] < 23 * 3600 and c.get("env") == ENV:
            print("  토큰 캐시 재사용")
            return c["token"]
    status, body = http("POST", "/oauth2/tokenP", body={
        "grant_type": "client_credentials",
        "appkey": APPKEY,
        "appsecret": APPSECRET,
    })
    if status != 200 or "access_token" not in body:
        sys.exit(f"토큰 발급 실패: HTTP {status} {body}")
    TOKEN_CACHE.write_text(json.dumps(
        {"token": body["access_token"], "issued_at": time.time(), "env": ENV}))
    print("  토큰 신규 발급 (24h 유효)")
    return body["access_token"]


def call_tr(token, tr_id, path, params, name):
    status, body = http("GET", path, headers={
        "authorization": f"Bearer {token}",
        "appkey": APPKEY,
        "appsecret": APPSECRET,
        "tr_id": tr_id,
        "custtype": "P",
    }, params=params)
    rt = body.get("rt_cd")
    ok = status == 200 and rt == "0"
    out = body.get("output") or body.get("output1") or []
    n = len(out) if isinstance(out, list) else 1
    print(f"  [{'OK' if ok else 'FAIL'}] {name} ({tr_id}) HTTP {status} rt_cd={rt} "
          f"msg={body.get('msg1', '')!r} rows={n}")
    if ok and n:
        sample = out[0] if isinstance(out, list) else out
        print(f"       응답 필드: {', '.join(list(sample.keys())[:12])} ...")
    RESULTS.append((name, tr_id, ok))
    return ok, body


def probe_rate_limit(token):
    """초당 k건 병렬 버스트를 올려가며 EGW00201(유량 초과) 발생 지점 실측."""
    print("\n== 초당 호출 한도 실측 (현재가 TR로 버스트) ==")
    path, tr = "/uapi/domestic-stock/v1/quotations/inquire-price", "FHKST01010100"
    p = {"FID_COND_MRKT_DIV_CODE": "J", "FID_INPUT_ISCD": "005930"}

    def one(_):
        s, b = http("GET", path, headers={
            "authorization": f"Bearer {token}", "appkey": APPKEY,
            "appsecret": APPSECRET, "tr_id": tr, "custtype": "P"}, params=p)
        return b.get("rt_cd") == "0", b.get("msg_cd", "")

    limit_found = None
    for k in (2, 5, 10, 15, 20):
        with ThreadPoolExecutor(max_workers=k) as ex:
            t0 = time.perf_counter()
            rs = list(ex.map(one, range(k)))
            dt = time.perf_counter() - t0
        fails = [m for ok, m in rs if not ok]
        rate_ok = not fails
        print(f"  {k:>2}건 동시 ({dt:.2f}s): {'전부 성공' if rate_ok else f'실패 {len(fails)}건 {set(fails)}'}")
        if not rate_ok:
            limit_found = k
            break
        time.sleep(2)  # 다음 버스트 전 여유
    print(f"  → 실측 한도: {'초당 ' + str(limit_found) + '건 미만에서 차단' if limit_found else '초당 20건까지 통과'}")
    return limit_found


def j2_budget(per_sec):
    """J2(30분 배치, 40종목) 호출 예산 계산."""
    calls = 2 + 40 * 2  # 순위 2회 + 종목당 (현재가 1 + 수급 1)
    dur = calls / max(per_sec, 1)
    print(f"\n== J2 예산: 배치당 약 {calls}회 호출 → 초당 {per_sec}건 기준 최소 {dur:.0f}초 소요 "
          f"({'여유' if dur < 600 else '30분 주기 내 빠듯 — 종목 수/주기 조정 검토'})")


def main():
    if not APPKEY or not APPSECRET:
        sys.exit("KIS_APPKEY / KIS_APPSECRET 환경변수를 설정하세요.")
    print(f"== 환경: {ENV} ({BASE}) ==\n\n== 1. 토큰 발급 ==")
    token = get_token()

    print("\n== 2. 순위 TR (실전 전용) ==")
    call_tr(token, "FHPST01700000", "/uapi/domestic-stock/v1/ranking/fluctuation", {
        "fid_cond_mrkt_div_code": "J", "fid_cond_scr_div_code": "20170",
        "fid_input_iscd": "0000", "fid_rank_sort_cls_code": "0",  # 0:상승률순 (1자 — "0000"은 SIZE 오류)
        "fid_input_cnt_1": "0", "fid_prc_cls_code": "0",  # 0:당일 누적
        "fid_input_price_1": "", "fid_input_price_2": "",
        "fid_vol_cnt": "", "fid_trgt_cls_code": "0",
        "fid_trgt_exls_cls_code": "0", "fid_div_cls_code": "0",
        "fid_rsfl_rate1": "", "fid_rsfl_rate2": ""}, "등락률 순위")
    time.sleep(1)
    call_tr(token, "FHPST01710000", "/uapi/domestic-stock/v1/quotations/volume-rank", {
        "FID_COND_MRKT_DIV_CODE": "J", "FID_COND_SCR_DIV_CODE": "20171",
        "FID_INPUT_ISCD": "0000", "FID_DIV_CLS_CODE": "0",
        "FID_BLNG_CLS_CODE": "3",  # 3=거래금액순 (특징주 선정 기준)
        "FID_TRGT_CLS_CODE": "111111111", "FID_TRGT_EXLS_CLS_CODE": "0000000000",
        "FID_INPUT_PRICE_1": "", "FID_INPUT_PRICE_2": "",
        "FID_VOL_CNT": "", "FID_INPUT_DATE_1": ""}, "거래량순위(거래대금)")

    print("\n== 3. 수급 TR ==")
    time.sleep(1)
    call_tr(token, "FHKST01010900", "/uapi/domestic-stock/v1/quotations/inquire-investor", {
        "FID_COND_MRKT_DIV_CODE": "J", "FID_INPUT_ISCD": "005930"},
        "종목별 투자자 (⚠️ 당일치는 장마감 후)")
    time.sleep(1)
    call_tr(token, "FHPTJ04400000", "/uapi/domestic-stock/v1/quotations/foreign-institution-total", {
        "FID_COND_MRKT_DIV_CODE": "V", "FID_COND_SCR_DIV_CODE": "16449",
        "FID_INPUT_ISCD": "0000", "FID_DIV_CLS_CODE": "0",
        "FID_RANK_SORT_CLS_CODE": "0", "FID_ETC_CLS_CODE": "0"},
        "외인·기관 매매 가집계 (장중 잠정용)")

    limit = None
    if "--probe" in sys.argv:
        limit = probe_rate_limit(token)
    j2_budget((limit - 1) if limit else 20)

    print("\n== 결과 요약 ==")
    for name, tr, ok in RESULTS:
        print(f"  {'✅' if ok else '❌'} {name} [{tr}]")
    print("\n응답 필드는 상세스펙 5장 표에 기입할 것.")


if __name__ == "__main__":
    main()
