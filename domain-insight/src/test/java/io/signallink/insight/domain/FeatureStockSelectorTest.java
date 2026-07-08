package io.signallink.insight.domain;

import static org.assertj.core.api.Assertions.assertThat;

import io.signallink.market.application.port.out.StockDaySnapshot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 특징주 선정 규칙 — ±5% 또는 거래량 3배 → 거래대금 상위 N. */
class FeatureStockSelectorTest {

    private static final LocalDate D = LocalDate.of(2026, 7, 8);

    private static StockDaySnapshot snap(String code, String changeRate, String volRatio, long tradingValue) {
        return new StockDaySnapshot(code, new BigDecimal(changeRate),
            volRatio == null ? null : new BigDecimal(volRatio), tradingValue);
    }

    @Test
    void 등락률_5퍼센트_이상이면_SURGE로_선정() {
        var out = FeatureStockSelector.select(List.of(snap("000001", "6.0", "1.0", 100)), D, 40);
        assertThat(out).singleElement()
            .satisfies(c -> assertThat(c.triggerType()).isEqualTo(TriggerType.SURGE));
    }

    @Test
    void 하락도_절대값_5퍼센트면_SURGE() {
        var out = FeatureStockSelector.select(List.of(snap("000001", "-6.0", null, 100)), D, 40);
        assertThat(out).singleElement()
            .satisfies(c -> assertThat(c.triggerType()).isEqualTo(TriggerType.SURGE));
    }

    @Test
    void 거래량_3배_이상이면_VOLUME으로_선정() {
        var out = FeatureStockSelector.select(List.of(snap("000001", "1.0", "3.5", 100)), D, 40);
        assertThat(out).singleElement()
            .satisfies(c -> assertThat(c.triggerType()).isEqualTo(TriggerType.VOLUME));
    }

    @Test
    void 둘_다_해당하면_SURGE_우선() {
        var out = FeatureStockSelector.select(List.of(snap("000001", "7.0", "4.0", 100)), D, 40);
        assertThat(out).singleElement()
            .satisfies(c -> assertThat(c.triggerType()).isEqualTo(TriggerType.SURGE));
    }

    @Test
    void 트리거_미달_종목은_제외() {
        var out = FeatureStockSelector.select(List.of(snap("000001", "2.0", "1.5", 999)), D, 40);
        assertThat(out).isEmpty();
    }

    @Test
    void vol_ratio가_null이면_거래량_트리거는_무시() {
        var out = FeatureStockSelector.select(List.of(snap("000001", "1.0", null, 100)), D, 40);
        assertThat(out).isEmpty();
    }

    @Test
    void 거래대금_상위_N만_큰_순서로_선정() {
        var out = FeatureStockSelector.select(List.of(
            snap("000001", "6.0", null, 100),
            snap("000002", "6.0", null, 300),
            snap("000003", "6.0", null, 200)), D, 2);
        assertThat(out).extracting(FeatureCandidate::stockCode).containsExactly("000002", "000003");
    }
}
