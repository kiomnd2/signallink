package io.signallink.market.adapter.out.persistence;

import io.signallink.market.application.port.out.StockRepositoryPort;
import io.signallink.market.domain.Stock;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 아웃바운드 어댑터 — StockRepositoryPort를 Spring Data JPA로 구현. */
@Component
@RequiredArgsConstructor
public class StockPersistenceAdapter implements StockRepositoryPort {

    private final StockJpaRepository jpa;

    @Override
    public Stock save(Stock stock) {
        return jpa.save(stock);
    }

    @Override
    public void saveAll(Collection<Stock> stocks) {
        jpa.saveAll(stocks);
    }

    @Override
    public long count() {
        return jpa.count();
    }

    @Override
    public List<Stock> findAll() {
        return jpa.findAll();
    }
}
