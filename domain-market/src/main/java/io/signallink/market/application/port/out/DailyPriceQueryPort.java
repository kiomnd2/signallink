package io.signallink.market.application.port.out;

import java.util.List;

/** 아웃바운드 포트 — 일봉 조회(베타·vol_ratio 등 계산용). 최신 거래일부터 limit개 종가. */
public interface DailyPriceQueryPort {
    List<DatedClose> recentCloses(String stockCode, int limit);
}
