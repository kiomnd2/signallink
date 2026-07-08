package io.signallink.market.application.port.out;

import java.time.LocalDate;
import java.util.List;

/** 아웃바운드 포트 — KIS 일별시세 조회. 구현: adapter.out.external.kis.KisMarketClient. */
public interface DailyPriceGatewayPort {
    List<DailyPriceData> fetchDailyPrices(String stockCode, LocalDate from, LocalDate to);
}
