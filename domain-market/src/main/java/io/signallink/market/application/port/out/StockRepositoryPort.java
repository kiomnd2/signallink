package io.signallink.market.application.port.out;

import io.signallink.market.domain.Stock;
import java.util.Collection;
import java.util.List;

/** 아웃바운드 포트 — 종목 마스터 영속화. 구현: adapter.out.persistence.StockPersistenceAdapter. */
public interface StockRepositoryPort {
    Stock save(Stock stock);
    void saveAll(Collection<Stock> stocks);
    long count();
    List<Stock> findAll();
}
