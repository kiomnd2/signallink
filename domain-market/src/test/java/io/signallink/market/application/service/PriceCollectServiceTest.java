package io.signallink.market.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.signallink.market.application.port.out.DailyPriceData;
import io.signallink.market.application.port.out.DailyPriceGatewayPort;
import io.signallink.market.application.port.out.DailyPriceRepositoryPort;
import io.signallink.market.application.port.out.IndexPriceGatewayPort;
import io.signallink.market.application.port.out.IndexPriceRepositoryPort;
import io.signallink.market.domain.DailyPrice;
import io.signallink.market.domain.IndexPrice;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 시세 수집 유스케이스 — 기존 날짜 스킵(멱등)을 포트 스텁으로 검증. */
class PriceCollectServiceTest {

    private static final LocalDate D0 = LocalDate.of(2026, 7, 8);
    private static final LocalDate D1 = LocalDate.of(2026, 7, 7);

    @Test
    void 일봉_수집시_이미_있는_날짜는_스킵한다() {
        DailyPriceGatewayPort dailyGw = (code, from, to) -> List.of(
            new DailyPriceData(D0, new BigDecimal("278000"), new BigDecimal("-6.081"), 100L, 200L),
            new DailyPriceData(D1, new BigDecimal("296000"), new BigDecimal("1.718"), 90L, 180L));
        IndexPriceGatewayPort indexGw = (code, from, to) -> List.of();
        DailyRepo dailyRepo = new DailyRepo();
        dailyRepo.existing.add("005930|" + D0); // D0은 이미 존재
        IndexRepo indexRepo = new IndexRepo();

        PriceCollectService service = new PriceCollectService(dailyGw, indexGw, dailyRepo, indexRepo);
        int saved = service.collectDaily("005930", D1, D0);

        assertThat(saved).isEqualTo(1);
        assertThat(dailyRepo.saved).extracting(DailyPrice::getTradeDate).containsExactly(D1);
    }

    static class DailyRepo implements DailyPriceRepositoryPort {
        final List<DailyPrice> saved = new ArrayList<>();
        final Set<String> existing = new HashSet<>();

        @Override public boolean existsByStockCodeAndTradeDate(String stockCode, LocalDate tradeDate) {
            return existing.contains(stockCode + "|" + tradeDate);
        }
        @Override public boolean existsByStockCode(String stockCode) { return false; }
        @Override public DailyPrice save(DailyPrice price) { saved.add(price); return price; }
        @Override public void saveAll(Collection<DailyPrice> prices) { saved.addAll(prices); }
    }

    static class IndexRepo implements IndexPriceRepositoryPort {
        final List<IndexPrice> saved = new ArrayList<>();

        @Override public boolean existsByIndexCodeAndTradeDate(String indexCode, LocalDate tradeDate) { return false; }
        @Override public void saveAll(Collection<IndexPrice> prices) { saved.addAll(prices); }
    }
}
