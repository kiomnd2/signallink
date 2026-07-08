package io.signallink.issue.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.signallink.issue.application.port.out.WatchStock;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 시드 CSV(seed/watchlist.csv)를 읽어 워치리스트를 구성하는지 검증. */
class SeedWatchlistAdapterTest {

    @Test
    void 시드_CSV를_읽어_헤더_제외하고_워치리스트를_반환한다() {
        List<WatchStock> stocks = new SeedWatchlistAdapter().watchlist();

        assertThat(stocks).hasSize(15);
        assertThat(stocks.get(0)).isEqualTo(new WatchStock("005930", "삼성전자"));
        assertThat(stocks).extracting(WatchStock::code).contains("035420", "035720");
    }
}
