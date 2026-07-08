package io.signallink.market.application.port.out;

import io.signallink.market.domain.DailyPrice;
import java.time.LocalDate;
import java.util.Collection;

/** 아웃바운드 포트 — 일봉 영속화. */
public interface DailyPriceRepositoryPort {
    boolean existsByStockCodeAndTradeDate(String stockCode, LocalDate tradeDate);
    boolean existsByStockCode(String stockCode);
    DailyPrice save(DailyPrice price);
    void saveAll(Collection<DailyPrice> prices);
}
