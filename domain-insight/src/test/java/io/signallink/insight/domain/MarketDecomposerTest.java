package io.signallink.insight.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** 시장분해 — market_contrib = β × 지수등락률, 초과분으로 동인 판정. */
class MarketDecomposerTest {

    @Test
    void 시장_몫이_더_크면_MARKET_DRIVEN() {
        var d = MarketDecomposer.decompose(new BigDecimal("2.1"), new BigDecimal("1.0"), new BigDecimal("2.0"));
        assertThat(d.marketContrib()).isEqualByComparingTo("2.000");
        assertThat(d.excess()).isEqualByComparingTo("0.100");
        assertThat(d.driver()).isEqualTo(Driver.MARKET_DRIVEN);
    }

    @Test
    void 개별_초과분이_더_크면_STOCK_SPECIFIC() {
        var d = MarketDecomposer.decompose(new BigDecimal("6.0"), new BigDecimal("1.0"), new BigDecimal("1.0"));
        assertThat(d.marketContrib()).isEqualByComparingTo("1.000");
        assertThat(d.excess()).isEqualByComparingTo("5.000");
        assertThat(d.driver()).isEqualTo(Driver.STOCK_SPECIFIC);
    }

    @Test
    void 베타가_시장영향을_증폭한다() {
        var d = MarketDecomposer.decompose(new BigDecimal("3.2"), new BigDecimal("1.5"), new BigDecimal("2.0"));
        assertThat(d.marketContrib()).isEqualByComparingTo("3.000"); // 1.5 × 2.0
    }

    @Test
    void 베타나_지수가_없으면_개별로_처리() {
        var d = MarketDecomposer.decompose(new BigDecimal("4.0"), null, new BigDecimal("2.0"));
        assertThat(d.marketContrib()).isNull();
        assertThat(d.excess()).isEqualByComparingTo("4.0");
        assertThat(d.driver()).isEqualTo(Driver.STOCK_SPECIFIC);
    }
}
