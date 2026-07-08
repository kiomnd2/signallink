package io.signallink.market.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.signallink.market.application.port.out.StockMasterEntry;
import io.signallink.market.application.port.out.StockMasterGatewayPort;
import io.signallink.market.application.port.out.StockRepositoryPort;
import io.signallink.market.domain.Stock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 종목마스터 동기화 — 게이트웨이 항목을 Stock으로 저장(포트 스텁, 외부 파일/DB 없음). */
class StockMasterSyncServiceTest {

    @Test
    void 게이트웨이_항목을_Stock으로_저장하고_수를_반환한다() {
        List<StockMasterEntry> entries = List.of(
            new StockMasterEntry("005930", "삼성전자", "KOSPI", null),
            new StockMasterEntry("035720", "카카오", "KOSPI", null));
        StockMasterGatewayPort gateway = () -> entries;
        CapturingRepo repo = new CapturingRepo();

        int synced = new StockMasterSyncService(gateway, repo).sync();

        assertThat(synced).isEqualTo(2);
        assertThat(repo.saved).extracting(Stock::getStockCode).containsExactly("005930", "035720");
        assertThat(repo.saved.get(0).getUpdatedAt()).isNotNull();
    }

    /** 포트 스텁: 저장 캡처. */
    static class CapturingRepo implements StockRepositoryPort {
        final List<Stock> saved = new ArrayList<>();

        @Override public Stock save(Stock stock) { saved.add(stock); return stock; }
        @Override public void saveAll(Collection<Stock> stocks) { saved.addAll(stocks); }
        @Override public long count() { return saved.size(); }
        @Override public java.util.List<Stock> findAll() { return saved; }
    }
}
