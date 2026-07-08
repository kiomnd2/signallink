package io.signallink.market.adapter.out.persistence;

import io.signallink.market.application.port.out.StockBetaRepositoryPort;
import io.signallink.market.domain.StockBeta;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 아웃바운드 어댑터 — 베타 저장(stock_code PK 기준 save=upsert). */
@Component
@RequiredArgsConstructor
public class StockBetaPersistenceAdapter implements StockBetaRepositoryPort {

    private final StockBetaJpaRepository jpa;

    @Override
    public void save(StockBeta beta) {
        jpa.save(beta);
    }
}
