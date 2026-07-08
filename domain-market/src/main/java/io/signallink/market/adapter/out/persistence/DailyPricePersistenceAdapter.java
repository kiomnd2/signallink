package io.signallink.market.adapter.out.persistence;

import io.signallink.market.application.port.out.DailyPriceRepositoryPort;
import io.signallink.market.domain.DailyPrice;
import java.time.LocalDate;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 아웃바운드 어댑터 — DailyPriceRepositoryPort를 Spring Data JPA로 구현. */
@Component
@RequiredArgsConstructor
public class DailyPricePersistenceAdapter implements DailyPriceRepositoryPort {

    private final DailyPriceJpaRepository jpa;

    @Override
    public boolean existsByStockCodeAndTradeDate(String stockCode, LocalDate tradeDate) {
        return jpa.existsByStockCodeAndTradeDate(stockCode, tradeDate);
    }

    @Override
    public DailyPrice save(DailyPrice price) {
        return jpa.save(price);
    }

    @Override
    public void saveAll(Collection<DailyPrice> prices) {
        jpa.saveAll(prices);
    }
}
