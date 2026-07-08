package io.signallink.market.application.port.out;

import java.util.List;

/** 아웃바운드 포트 — 지수 조회(베타 계산용). 최신 거래일부터 limit개 종가. */
public interface IndexPriceQueryPort {
    List<DatedClose> recentCloses(String indexCode, int limit);
}
