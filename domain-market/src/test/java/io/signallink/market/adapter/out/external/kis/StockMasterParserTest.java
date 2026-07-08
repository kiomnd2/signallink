package io.signallink.market.adapter.out.external.kis;

import static org.assertj.core.api.Assertions.assertThat;

import io.signallink.market.application.port.out.StockMasterEntry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** KIS .mst 파서 — 실제 포맷 픽스처(주식/ETF/ETN 각 1)로 주식만 남는지 검증. */
class StockMasterParserTest {

    @Test
    void 주식만_남기고_ETF_ETN은_제외한다() throws Exception {
        byte[] mst;
        try (var in = new ClassPathResource("fixtures/stock-master-sample.mst").getInputStream()) {
            mst = in.readAllBytes();
        }

        List<StockMasterEntry> stocks = StockMasterParser.parse(mst, "KOSPI");

        assertThat(stocks).hasSize(1); // 삼성전자(ST)만 — KODEX(EF)·ETN(EN) 제외
        StockMasterEntry samsung = stocks.get(0);
        assertThat(samsung.code()).isEqualTo("005930");
        assertThat(samsung.name()).isEqualTo("삼성전자");
        assertThat(samsung.marketType()).isEqualTo("KOSPI");
        assertThat(samsung.sectorCode()).isNull();
    }
}
