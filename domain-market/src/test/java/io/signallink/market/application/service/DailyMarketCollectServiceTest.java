package io.signallink.market.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.signallink.common.TimeUtils;
import io.signallink.market.application.port.out.DailyPriceData;
import io.signallink.market.application.port.out.DailyPriceGatewayPort;
import io.signallink.market.application.port.out.DailyPriceRepositoryPort;
import io.signallink.market.application.port.out.IndexPriceData;
import io.signallink.market.application.port.out.IndexPriceGatewayPort;
import io.signallink.market.application.port.out.IndexPriceRepositoryPort;
import io.signallink.market.application.port.out.StockRepositoryPort;
import io.signallink.market.domain.DailyPrice;
import io.signallink.market.domain.IndexPrice;
import io.signallink.market.domain.Stock;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 일일 수집 — 유니버스 일봉 + 지수를 합산 수집하는지 포트 스텁으로 검증. */
class DailyMarketCollectServiceTest {

    @Test
    void 유니버스_일봉과_지수를_수집한다() {
        StubStockRepo stockRepo = new StubStockRepo(List.of(stock("005930"), stock("000660")));
        DailyPriceGatewayPort dailyGw = (code, from, to) ->
            List.of(new DailyPriceData(TimeUtils.todayKst(), new BigDecimal("100"), BigDecimal.ZERO, 1L, 1L));
        IndexPriceGatewayPort indexGw = (code, from, to) ->
            List.of(new IndexPriceData(TimeUtils.todayKst(), new BigDecimal("2500"), BigDecimal.ZERO));
        PriceCollectService collect = new PriceCollectService(dailyGw, indexGw, new DailyRepo(), new IndexRepo());

        int total = new DailyMarketCollectService(stockRepo, collect).collectAll(10, 5, List.of("0001"));

        assertThat(total).isEqualTo(3); // 종목 2 + 지수 1
    }

    private static Stock stock(String code) {
        return new Stock(code, "종목" + code, "KOSPI", null, OffsetDateTime.parse("2026-07-08T09:00+09:00"));
    }

    static class StubStockRepo implements StockRepositoryPort {
        private final List<Stock> stocks;
        StubStockRepo(List<Stock> stocks) { this.stocks = stocks; }
        @Override public Stock save(Stock s) { return s; }
        @Override public void saveAll(Collection<Stock> s) {}
        @Override public long count() { return stocks.size(); }
        @Override public List<Stock> findAll() { return stocks; }
    }

    static class DailyRepo implements DailyPriceRepositoryPort {
        final Set<String> saved = new HashSet<>();
        @Override public boolean existsByStockCodeAndTradeDate(String c, LocalDate d) { return saved.contains(c + "|" + d); }
        @Override public boolean existsByStockCode(String c) { return false; }
        @Override public DailyPrice save(DailyPrice p) { return p; }
        @Override public void saveAll(Collection<DailyPrice> ps) { ps.forEach(p -> saved.add(p.getStockCode() + "|" + p.getTradeDate())); }
    }

    static class IndexRepo implements IndexPriceRepositoryPort {
        @Override public boolean existsByIndexCodeAndTradeDate(String c, LocalDate d) { return false; }
        @Override public void saveAll(Collection<IndexPrice> ps) {}
    }
}
