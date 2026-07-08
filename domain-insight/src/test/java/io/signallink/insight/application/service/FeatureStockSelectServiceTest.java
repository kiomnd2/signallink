package io.signallink.insight.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.signallink.insight.domain.FeatureCandidate;
import io.signallink.insight.domain.TriggerType;
import io.signallink.market.application.port.out.StockDaySnapshot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 특징주 선정 유스케이스 — 조회 포트 스텁으로 선정 결과를 검증. */
class FeatureStockSelectServiceTest {

    private static final LocalDate D = LocalDate.of(2026, 7, 8);

    @Test
    void 조회한_스냅샷에서_트리거_종목만_거래대금순으로_선정() {
        List<StockDaySnapshot> snapshots = List.of(
            new StockDaySnapshot("000001", new BigDecimal("6.0"), null, 100),   // SURGE
            new StockDaySnapshot("000002", new BigDecimal("1.0"), new BigDecimal("3.5"), 300), // VOLUME
            new StockDaySnapshot("000003", new BigDecimal("2.0"), new BigDecimal("1.2"), 999)); // 미달
        var service = new FeatureStockSelectService(date -> snapshots);

        List<FeatureCandidate> out = service.select(D);

        assertThat(out).extracting(FeatureCandidate::stockCode).containsExactly("000002", "000001");
        assertThat(out).extracting(FeatureCandidate::triggerType)
            .containsExactly(TriggerType.VOLUME, TriggerType.SURGE);
    }
}
