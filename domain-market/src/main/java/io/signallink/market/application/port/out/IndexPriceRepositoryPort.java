package io.signallink.market.application.port.out;

import io.signallink.market.domain.IndexPrice;
import java.time.LocalDate;
import java.util.Collection;

/** 아웃바운드 포트 — 지수 시세 영속화. */
public interface IndexPriceRepositoryPort {
    boolean existsByIndexCodeAndTradeDate(String indexCode, LocalDate tradeDate);
    void saveAll(Collection<IndexPrice> prices);
}
