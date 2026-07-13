package io.signallink.market.adapter.out.persistence;

import io.signallink.market.application.port.out.StockBetaQueryPort;
import io.signallink.market.application.port.out.StockBetaRepositoryPort;
import io.signallink.market.domain.StockBeta;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 아웃바운드 어댑터 — 베타 저장(stock_code PK 기준 save=upsert)·조회. */
@Component
@RequiredArgsConstructor
public class StockBetaPersistenceAdapter implements StockBetaRepositoryPort, StockBetaQueryPort {

    private final StockBetaJpaRepository jpa;

    @Override
    public void save(StockBeta beta) {
        jpa.save(beta);
    }

    @Override
    public BigDecimal beta60d(String stockCode) {
        return jpa.findById(stockCode).map(StockBeta::getBeta60d).orElse(null);
    }
}
