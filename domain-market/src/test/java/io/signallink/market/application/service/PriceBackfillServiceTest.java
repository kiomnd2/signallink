package io.signallink.market.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.signallink.market.application.port.out.DailyPriceData;
import io.signallink.market.application.port.out.DailyPriceGatewayPort;
import io.signallink.market.application.port.out.DailyPriceRepositoryPort;
import io.signallink.market.application.port.out.IndexPriceGatewayPort;
import io.signallink.market.application.port.out.IndexPriceRepositoryPort;
import io.signallink.market.domain.DailyPrice;
import io.signallink.market.domain.IndexPrice;
import io.signallink.market.domain.Stock;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 백필 — 기간을 100일 창으로 분할하고, 이미 데이터 있는 종목은 스킵하는지 검증. */
class PriceBackfillServiceTest {

    private static final LocalDate FROM = LocalDate.of(2024, 1, 1);
    private static final LocalDate TO = LocalDate.of(2024, 12, 31); // 366일

    @Test
    void 기간을_100일_창으로_나눠_수집한다() {
        StubStockRepo stockRepo = new StubStockRepo(List.of(stock("005930")));
        RecordingGateway gateway = new RecordingGateway();
        DailyRepo dailyRepo = new DailyRepo();
        PriceCollectService collect = new PriceCollectService(gateway, emptyIndexGateway(), dailyRepo, new IndexRepo());

        int total = new PriceBackfillService(stockRepo, dailyRepo, collect).backfill(10, FROM, TO);

        // 366일 → 100일 창 4개 (100,100,100,66)
        assertThat(gateway.windows).hasSize(4);
        assertThat(gateway.windows.get(0).from()).isEqualTo(FROM);
        assertThat(gateway.windows.get(3).to()).isEqualTo(TO);
        for (Window w : gateway.windows) {
            assertThat(ChronoUnit.DAYS.between(w.from(), w.to())).isLessThan(100); // ≤100일
        }
        // 창마다 신규 1건 → 4건
        assertThat(total).isEqualTo(4);
    }

    @Test
    void 이미_데이터가_있는_종목은_스킵한다() {
        StubStockRepo stockRepo = new StubStockRepo(List.of(stock("005930")));
        RecordingGateway gateway = new RecordingGateway();
        DailyRepo dailyRepo = new DailyRepo();
        dailyRepo.backfilled.add("005930"); // 이미 백필됨
        PriceCollectService collect = new PriceCollectService(gateway, emptyIndexGateway(), dailyRepo, new IndexRepo());

        int total = new PriceBackfillService(stockRepo, dailyRepo, collect).backfill(10, FROM, TO);

        assertThat(gateway.windows).isEmpty(); // 호출 없음
        assertThat(total).isZero();
    }

    private static Stock stock(String code) {
        return new Stock(code, "종목" + code, "KOSPI", null, OffsetDateTime.parse("2026-07-08T09:00+09:00"));
    }

    private static IndexPriceGatewayPort emptyIndexGateway() {
        return (code, from, to) -> List.of();
    }

    record Window(LocalDate from, LocalDate to) {}

    static class RecordingGateway implements DailyPriceGatewayPort {
        final List<Window> windows = new ArrayList<>();
        @Override public List<DailyPriceData> fetchDailyPrices(String stockCode, LocalDate from, LocalDate to) {
            windows.add(new Window(from, to));
            return List.of(new DailyPriceData(from, new BigDecimal("100"), BigDecimal.ZERO, 1L, 1L));
        }
    }

    static class StubStockRepo implements io.signallink.market.application.port.out.StockRepositoryPort {
        private final List<Stock> stocks;
        StubStockRepo(List<Stock> stocks) { this.stocks = stocks; }
        @Override public Stock save(Stock s) { return s; }
        @Override public void saveAll(Collection<Stock> s) {}
        @Override public long count() { return stocks.size(); }
        @Override public List<Stock> findAll() { return stocks; }
    }

    static class DailyRepo implements DailyPriceRepositoryPort {
        final Set<String> savedDates = new HashSet<>();
        final Set<String> backfilled = new HashSet<>();
        @Override public boolean existsByStockCodeAndTradeDate(String code, LocalDate d) { return savedDates.contains(code + "|" + d); }
        @Override public boolean existsByStockCode(String code) { return backfilled.contains(code); }
        @Override public DailyPrice save(DailyPrice p) { return p; }
        @Override public void saveAll(Collection<DailyPrice> ps) { ps.forEach(p -> savedDates.add(p.getStockCode() + "|" + p.getTradeDate())); }
    }

    static class IndexRepo implements IndexPriceRepositoryPort {
        @Override public boolean existsByIndexCodeAndTradeDate(String code, LocalDate d) { return false; }
        @Override public void saveAll(Collection<IndexPrice> ps) {}
    }
}
