package io.signallink.market.adapter.out.persistence;

import io.signallink.market.application.port.out.DatedClose;
import io.signallink.market.application.port.out.IndexChangeQueryPort;
import io.signallink.market.application.port.out.IndexPriceQueryPort;
import io.signallink.market.application.port.out.IndexPriceRepositoryPort;
import io.signallink.market.domain.IndexPrice;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/** 아웃바운드 어댑터 — 지수 영속/조회를 Spring Data JPA로 구현. */
@Component
@RequiredArgsConstructor
public class IndexPricePersistenceAdapter
    implements IndexPriceRepositoryPort, IndexPriceQueryPort, IndexChangeQueryPort {

    private final IndexPriceJpaRepository jpa;

    @Override
    public boolean existsByIndexCodeAndTradeDate(String indexCode, LocalDate tradeDate) {
        return jpa.existsByIndexCodeAndTradeDate(indexCode, tradeDate);
    }

    @Override
    public void saveAll(Collection<IndexPrice> prices) {
        jpa.saveAll(prices);
    }

    @Override
    public List<DatedClose> recentCloses(String indexCode, int limit) {
        return jpa.findByIndexCodeOrderByTradeDateDesc(indexCode, PageRequest.of(0, limit)).stream()
            .map(p -> new DatedClose(p.getTradeDate(), p.getClose()))
            .toList();
    }

    @Override
    public BigDecimal changeRateOn(String indexCode, LocalDate tradeDate) {
        return jpa.findByIndexCodeAndTradeDate(indexCode, tradeDate)
            .map(IndexPrice::getChangeRate)
            .orElse(null);
    }
}
