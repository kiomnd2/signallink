package io.signallink.market.application.port.out;

import java.time.LocalDate;
import java.util.List;

/** 아웃바운드 포트 — KIS 지수 일별시세 조회. 구현: adapter.out.external.kis.KisMarketClient. */
public interface IndexPriceGatewayPort {
    List<IndexPriceData> fetchIndexPrices(String indexCode, LocalDate from, LocalDate to);
}
