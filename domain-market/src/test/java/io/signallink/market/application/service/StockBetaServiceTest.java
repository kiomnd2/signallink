package io.signallink.market.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.signallink.market.application.port.out.DailyPriceQueryPort;
import io.signallink.market.application.port.out.DatedClose;
import io.signallink.market.application.port.out.IndexPriceQueryPort;
import io.signallink.market.application.port.out.StockBetaRepositoryPort;
import io.signallink.market.application.port.out.StockRepositoryPort;
import io.signallink.market.domain.Stock;
import io.signallink.market.domain.StockBeta;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 베타 서비스 — 유니버스 종목의 베타를 계산·저장(포트 스텁). 종목=지수의 2배 → β=2.0. */
class StockBetaServiceTest {

    private static final double[] R = {
        0.01, -0.02, 0.015, -0.005, 0.02, -0.01, 0.008, -0.012, 0.006, -0.003,
        0.011, -0.007, 0.004, -0.009, 0.013, -0.006, 0.002, -0.011, 0.007, -0.004,
        0.009, -0.002, 0.005, -0.008
    };

    @Test
    void 유니버스_종목의_베타를_계산_저장한다() {
        List<DatedClose> index = new ArrayList<>();
        List<DatedClose> stock = new ArrayList<>();
        LocalDate d = LocalDate.of(2026, 1, 1);
        double idx = 100.0;
        double stk = 100.0;
        index.add(new DatedClose(d, BigDecimal.valueOf(idx)));
        stock.add(new DatedClose(d, BigDecimal.valueOf(stk)));
        for (double r : R) {
            d = d.plusDays(1);
            idx *= (1 + r);
            stk *= (1 + 2 * r);
            index.add(new DatedClose(d, BigDecimal.valueOf(idx)));
            stock.add(new DatedClose(d, BigDecimal.valueOf(stk)));
        }

        StubStockRepo stockRepo = new StubStockRepo(List.of(
            new Stock("005930", "삼성전자", "KOSPI", null, OffsetDateTime.parse("2026-07-08T09:00+09:00"))));
        DailyPriceQueryPort dailyQuery = (code, limit) -> stock;
        IndexPriceQueryPort indexQuery = (code, limit) -> index;
        CapturingBetaRepo betaRepo = new CapturingBetaRepo();

        int done = new StockBetaService(stockRepo, dailyQuery, indexQuery, betaRepo).calculateAll(10);

        assertThat(done).isEqualTo(1);
        assertThat(betaRepo.saved).hasSize(1);
        assertThat(betaRepo.saved.get(0).getStockCode()).isEqualTo("005930");
        assertThat(betaRepo.saved.get(0).getBeta60d()).isEqualByComparingTo("2.000");
    }

    static class StubStockRepo implements StockRepositoryPort {
        private final List<Stock> stocks;
        StubStockRepo(List<Stock> stocks) { this.stocks = stocks; }
        @Override public Stock save(Stock s) { return s; }
        @Override public void saveAll(Collection<Stock> s) {}
        @Override public long count() { return stocks.size(); }
        @Override public List<Stock> findAll() { return stocks; }
    }

    static class CapturingBetaRepo implements StockBetaRepositoryPort {
        final List<StockBeta> saved = new ArrayList<>();
        @Override public void save(StockBeta beta) { saved.add(beta); }
    }
}
