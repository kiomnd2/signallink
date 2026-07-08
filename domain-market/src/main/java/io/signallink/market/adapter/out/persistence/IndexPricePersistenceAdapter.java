package io.signallink.market.adapter.out.persistence;

import io.signallink.market.application.port.out.IndexPriceRepositoryPort;
import io.signallink.market.domain.IndexPrice;
import java.time.LocalDate;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 아웃바운드 어댑터 — IndexPriceRepositoryPort를 Spring Data JPA로 구현. */
@Component
@RequiredArgsConstructor
public class IndexPricePersistenceAdapter implements IndexPriceRepositoryPort {

    private final IndexPriceJpaRepository jpa;

    @Override
    public boolean existsByIndexCodeAndTradeDate(String indexCode, LocalDate tradeDate) {
        return jpa.existsByIndexCodeAndTradeDate(indexCode, tradeDate);
    }

    @Override
    public void saveAll(Collection<IndexPrice> prices) {
        jpa.saveAll(prices);
    }
}
