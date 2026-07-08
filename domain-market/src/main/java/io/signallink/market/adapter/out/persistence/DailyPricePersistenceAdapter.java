package io.signallink.market.adapter.out.persistence;

import io.signallink.market.application.port.out.DailyPriceQueryPort;
import io.signallink.market.application.port.out.DailyPriceRepositoryPort;
import io.signallink.market.application.port.out.DatedClose;
import io.signallink.market.domain.DailyPrice;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/** 아웃바운드 어댑터 — 일봉 영속/조회를 Spring Data JPA로 구현. */
@Component
@RequiredArgsConstructor
public class DailyPricePersistenceAdapter implements DailyPriceRepositoryPort, DailyPriceQueryPort {

    private final DailyPriceJpaRepository jpa;

    @Override
    public boolean existsByStockCodeAndTradeDate(String stockCode, LocalDate tradeDate) {
        return jpa.existsByStockCodeAndTradeDate(stockCode, tradeDate);
    }

    @Override
    public boolean existsByStockCode(String stockCode) {
        return jpa.existsByStockCode(stockCode);
    }

    @Override
    public DailyPrice save(DailyPrice price) {
        return jpa.save(price);
    }

    @Override
    public void saveAll(Collection<DailyPrice> prices) {
        jpa.saveAll(prices);
    }

    @Override
    public List<DatedClose> recentCloses(String stockCode, int limit) {
        return jpa.findByStockCodeOrderByTradeDateDesc(stockCode, PageRequest.of(0, limit)).stream()
            .map(p -> new DatedClose(p.getTradeDate(), p.getClose()))
            .toList();
    }
}
