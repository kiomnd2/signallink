package io.signallink.market.application.port.out;

import io.signallink.market.domain.StockBeta;

/** 아웃바운드 포트 — 베타 영속화(stock_code PK, save가 upsert). */
public interface StockBetaRepositoryPort {
    void save(StockBeta beta);
}
